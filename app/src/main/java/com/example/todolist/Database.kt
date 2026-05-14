package com.example.todolist

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo val title: String,
    @ColumnInfo val description: String,
    @ColumnInfo val creationTime: String,
    @ColumnInfo val dueTime: String,
    @ColumnInfo var completed: Boolean,
    @ColumnInfo val showNotification: Boolean,
    @ColumnInfo val category: Int,
    // Location based notification fields
    @ColumnInfo val locationNotification: Boolean = false,
    @ColumnInfo val latitude: Double? = null,
    @ColumnInfo val longitude: Double? = null,
    @ColumnInfo val radiusMeters: Int? = null
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM Task ORDER BY dueTime ASC")
    fun getAll(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM Task WHERE uid = :id")
    suspend fun getTaskById(id: Int): Task?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Transaction
    @Query("SELECT * FROM Task ORDER BY dueTime ASC")
    fun getTasksWithAttachments(): Flow<List<TaskWithAttachments>>

    @Transaction
    @Query("SELECT * FROM Task WHERE uid = :id")
    suspend fun getTaskWithAttachmentsById(id: Int): TaskWithAttachments?

    @Transaction
    suspend fun saveTaskWithAttachments(task: Task, attachmentsToSave: List<Attachment>): Int {
        val taskId = save(task).toInt()

        val existingAttachments = getTaskWithAttachmentsById(taskId)?.attachments ?: emptyList()
        val attachmentsToRemove = existingAttachments.filter { it !in attachmentsToSave }
        attachmentsToRemove.forEach { deleteAttachment(it) }

        attachmentsToSave.forEach { attachment ->
            insertAttachment(attachment.copy(taskId = taskId))
        }

        return taskId
    }
}

@Database(entities = [Task::class, Attachment::class], version = 3, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var Instance: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, TaskDatabase::class.java, "task_database")
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["uid"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val fileName: String,
    val mimeType: String,
    val filePath: String
)

data class TaskWithAttachments(
    @Embedded val task: Task,
    @Relation(parentColumn = "uid", entityColumn = "taskId")
    val attachments: List<Attachment>
)