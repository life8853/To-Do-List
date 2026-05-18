package com.example.todolist

class TaskRepository(private val taskDao: TaskDao) : ITaskRepository {

    override fun getTasksWithAttachmentsFlow() = taskDao.getTasksWithAttachments()

    override suspend fun getTaskWithAttachmentsById(id: Int) =
        taskDao.getTaskWithAttachmentsById(id)

    override suspend fun getTaskById(id: Int) = taskDao.getTaskById(id)

    override suspend fun saveTaskWithAttachments(task: Task, attachments: List<Attachment>): Int {
        return taskDao.saveTaskWithAttachments(task, attachments)
    }

    override suspend fun deleteTask(task: Task) = taskDao.delete(task)

    override suspend fun updateTask(task: Task) = taskDao.update(task)
}

