package com.example.todolist

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getTasksWithAttachmentsFlow(): Flow<List<TaskWithAttachments>> = taskDao.getTasksWithAttachments()

    suspend fun getTaskWithAttachmentsById(id: Int): TaskWithAttachments? = taskDao.getTaskWithAttachmentsById(id)

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    suspend fun saveTaskWithAttachments(task: Task, attachments: List<Attachment>): Int {
        return taskDao.saveTaskWithAttachments(task, attachments)
    }

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)
}

