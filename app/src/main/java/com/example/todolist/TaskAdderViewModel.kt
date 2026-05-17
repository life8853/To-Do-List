package com.example.todolist

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import com.example.todolist.GeofenceManager
import com.example.todolist.TaskRepository
import com.example.todolist.NotificationService
import com.example.todolist.GeofenceService

class TaskAdderViewModel(
    application: Application,
    private val taskId: Int?,
    private val repository: TaskRepository = TaskRepository(TaskDatabase.getDatabase(application).taskDao()),
    private val notifier: NotificationService = NotificationScheduler(application),
    private val geofenceService: GeofenceService = GeofenceManager(application)
) : AndroidViewModel(application) {

    var title by mutableStateOf("")

    var description by mutableStateOf("")

    var notifyUser by mutableStateOf(false)
    
    // Notification time minutes before deadline (per-task setting)
    var notificationTime by mutableStateOf(8)

    // Location-based notification fields
    var locationNotification by mutableStateOf(false)
    var latitudeStr by mutableStateOf("")
    var longitudeStr by mutableStateOf("")
    var radiusStr by mutableStateOf("")

    var deadline by mutableStateOf<LocalDateTime?>(null)

    var category by mutableStateOf(Category.HEALTH)

    var isTaskEdited by mutableStateOf(false)

    val existingAttachments = mutableStateListOf<Attachment>()
    val draftAttachments = mutableStateListOf<DraftAttachment>()

    // repository, notifier and geofenceService are provided via constructor (with defaults)

    init {
        if (taskId != null && taskId != 0) {
            isTaskEdited = true
            viewModelScope.launch {
                repository.getTaskWithAttachmentsById(taskId)?.let { taskWithAttachments ->
                    val task = taskWithAttachments.task
                    title = task.title
                    description = task.description
                    notifyUser = task.showNotification
                    deadline = LocalDateTime.parse(task.dueTime)
                    category = Category.entries.find { it.id == task.category } ?: Category.HEALTH
                    existingAttachments.addAll(taskWithAttachments.attachments)
                    // load location fields
                    locationNotification = task.locationNotification
                    task.latitude?.let { latitudeStr = it.toString() }
                    task.longitude?.let { longitudeStr = it.toString() }
                    task.radiusMeters?.let { radiusStr = it.toString() }
                }
            }
        }
    }

    fun createTask(onComplete: () -> Unit) {

        if (deadline == null || title.isEmpty() || description.isEmpty()) {
            Toast.makeText(getApplication(), "Please fill all necessary fields", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val latVal = latitudeStr.toDoubleOrNull()
        val lonVal = longitudeStr.toDoubleOrNull()
        val radiusVal = radiusStr.toIntOrNull()

        val newTask =
            Task(
                uid = if (isTaskEdited) taskId!! else 0,
                title = title,
                description = description,
                creationTime = LocalDateTime.now().toString(),
                dueTime = deadline!!.toString(),
                completed = false,
                showNotification = notifyUser,
                category = category.id,
                locationNotification = locationNotification,
                latitude = latVal,
                longitude = lonVal,
                radiusMeters = radiusVal
            )

        val settings = SettingsManager.getInstance(getApplication())

        viewModelScope.launch {
            // Use per-task notification time if specified, otherwise fall back to global setting
            val notifyBeforeMinutes = if (notifyUser) {
                notificationTime
            } else {
                settings.settingsFlow.first().notificationTime
            }

            val newAttachments = draftAttachments.mapNotNull { draft ->
                performFileSync(draft, if (isTaskEdited) taskId else 0)
            }

            val allAttachments = existingAttachments + newAttachments

            val savedTaskId = repository.saveTaskWithAttachments(newTask, allAttachments)

            if (isTaskEdited) {
                notifier.cancelNotification(taskId!!)
                // remove any existing geofence for this task before re-adding
                try {
                    geofenceService.removeGeofence(taskId!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (newTask.showNotification) {
                notifier.scheduleTimeNotification(newTask.copy(uid = savedTaskId), notifyBeforeMinutes)
            }

            // Schedule geofence if location notification enabled and coordinates available
            if (newTask.locationNotification) {
                if (newTask.latitude != null && newTask.longitude != null && newTask.radiusMeters != null) {
                    try {
                        android.util.Log.d("TaskAdderViewModel", "Adding geofence: lat=${newTask.latitude}, lng=${newTask.longitude}, radius=${newTask.radiusMeters}m for task ID=$savedTaskId")
                        geofenceService.addGeofence(
                            requestId = savedTaskId,
                            latitude = newTask.latitude,
                            longitude = newTask.longitude,
                            radiusMeters = newTask.radiusMeters
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("TaskAdderViewModel", "Failed to add geofence: ${e.message}", e)
                        e.printStackTrace()
                    }
                } else {
                    android.util.Log.w("TaskAdderViewModel", "Location notification enabled but coordinates/radius missing: lat=${newTask.latitude}, lng=${newTask.longitude}, radius=${newTask.radiusMeters}")
                }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    data class DraftAttachment(
        val uri: Uri,
        val name: String,
        val mimeType: String
    )

    fun addAttachmentDraft(uri: Uri) {
        val context = getApplication<Application>()

        val name = "file_${System.currentTimeMillis()}"
        val mime = context.contentResolver.getType(uri) ?: "application/data"

        draftAttachments.add(DraftAttachment(uri, name, mime))
    }

    private suspend fun performFileSync(draft: DraftAttachment, taskId: Int?): Attachment? =
        withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(draft.uri)?.use { input ->
                    context.openFileOutput(draft.name, Context.MODE_PRIVATE).use { output ->
                        input.copyTo(output)
                    }
                }
                val file = File(context.filesDir, draft.name)

                Attachment(
                    taskId = taskId ?: 0,
                    fileName = draft.name,
                    filePath = file.absolutePath,
                    mimeType = draft.mimeType
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    fun openDraftAttachment(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with..."))
    }

    fun removeDraftAttachment(draftAttachment: DraftAttachment) {
        draftAttachments.remove(draftAttachment)
    }


    fun openAttachment(context: Context, attachment: Attachment) {
        val file = File(attachment.filePath)
        val uri = FileProvider.getUriForFile(context, "com.example.todolist.provider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Open with..."))
    }

    fun removeAttachment(attachment: Attachment) {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().deleteFile(attachment.fileName)
        }
        existingAttachments.remove(attachment)
    }
}