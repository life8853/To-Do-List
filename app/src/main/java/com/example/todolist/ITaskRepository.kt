package com.example.todolist

import kotlinx.coroutines.flow.Flow

interface ITaskRepository {
    fun getTasksWithAttachmentsFlow(): Flow<List<TaskWithAttachments>>
    suspend fun getTaskWithAttachmentsById(id: Int): TaskWithAttachments?
    suspend fun getTaskById(id: Int): Task?
    suspend fun saveTaskWithAttachments(task: Task, attachments: List<Attachment>): Int
    suspend fun deleteTask(task: Task)
    suspend fun updateTask(task: Task)
}