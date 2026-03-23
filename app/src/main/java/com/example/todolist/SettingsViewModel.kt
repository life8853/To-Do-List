package com.example.todolist

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    var currentSettings by mutableStateOf<UserSettings?>(null)

    val settingsManager = SettingsManager.getInstance(application)

    init {
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { settings -> currentSettings = settings }
        }
    }

    fun updateHideCompleted(hideCompleted: Boolean) {
        viewModelScope.launch { settingsManager.saveHideCompleted(hideCompleted) }
    }

    fun updateNotificationTime(minutes: Int) {
        viewModelScope.launch { settingsManager.saveNotificationTime(minutes) }
    }

    fun updateCategory(category: Category, isVisible: Boolean) {
        viewModelScope.launch {
            val currentCategories =
                currentSettings?.visibleCategories?.toMutableSet() ?: mutableSetOf()
            if (isVisible) currentCategories.add(category) else currentCategories.remove(category)
            settingsManager.saveCategories(currentCategories)
        }
    }
}