package com.example.todolist

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todolist.ui.theme.ToDoListTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAdder(modifier: Modifier = Modifier, taskID: Int? = null, onBack: () -> Unit) {

    val context = LocalContext.current

    val viewModel: TaskAdderViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskAdderViewModel(
                    application = context.applicationContext as Application,
                    taskId = taskID
                ) as T
            }
        }
    )

    val scrollState = rememberScrollState()

    var dropdownExpanded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addAttachmentDraft(it)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = Color(0xFFF5F5F5))
                .padding(innerPadding)
                .padding(8.dp, 8.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(12.dp, 8.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Task Details", fontSize = 24.sp)
                    }
                    OutlinedTextField(
                        viewModel.title,
                        { viewModel.title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        viewModel.description,
                        { viewModel.description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DatePickerInput(viewModel.deadline, onDateTimeSelected = { selectedDateTime ->
                        viewModel.deadline = selectedDateTime
                    })

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = viewModel.category.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            Category.entries.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName) },
                                    onClick = {
                                        viewModel.category = category
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send Notification")
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = viewModel.notifyUser,
                            onCheckedChange = {
                                viewModel.notifyUser = it
                            }
                        )
                    }

                    Text("Attachments")

                    viewModel.existingAttachments.forEach { attachment ->
                        AttachmentItem(
                            fileName = attachment.fileName,
                            onOpen = { viewModel.openAttachment(context, attachment) },
                            onDelete = { viewModel.removeAttachment(attachment = attachment) }
                        )
                    }

                    viewModel.draftAttachments.forEach { draft ->
                        AttachmentItem(
                            fileName = draft.name,
                            onOpen = {
                                viewModel.openDraftAttachment(
                                    context,
                                    draft.uri,
                                    draft.mimeType
                                )
                            },
                            onDelete = { viewModel.removeDraftAttachment(draft) }
                        )
                    }

                    OutlinedButton(
                        onClick = { launcher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Attachment")
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { viewModel.createTask(onBack) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (viewModel.isTaskEdited) "Save Changes" else "Create Task")
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun AttachmentItem(
    fileName: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = fileName,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskAdderPreview() {
    ToDoListTheme {
        TaskAdder(onBack = {})
    }
}