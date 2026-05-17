package com.example.todolist

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager private constructor(context: Context) {
    private val dataStore = context.dataStore

    private val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
    private val NOTIF_TIME = intPreferencesKey("notif_time")
    private val VISIBLE_CATEGORIES = stringSetPreferencesKey("visible_categories")

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            hideCompleted = preferences[HIDE_COMPLETED] ?: true,
            notificationTime = preferences[NOTIF_TIME] ?: 8,
            visibleCategories = preferences[VISIBLE_CATEGORIES]?.map { Category.valueOf(it) }
                ?.toSet()
                ?: Category.entries.toSet()
        )
    }

    suspend fun saveHideCompleted(hide: Boolean) {
        dataStore.edit { it[HIDE_COMPLETED] = hide }
    }

    suspend fun saveNotificationTime(minutesToNotification: Int) {
        dataStore.edit { it[NOTIF_TIME] = minutesToNotification }
    }

    suspend fun saveCategories(categories: Set<Category>) {
        dataStore.edit { it -> it[VISIBLE_CATEGORIES] = categories.map { it.name }.toSet() }
    }

    companion object {
        @Volatile
        private var Instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            if (Instance == null) {
                synchronized(this) {
                    if (Instance == null) {
                        Instance = SettingsManager(context)
                    }
                }
            }
            return Instance!!
        }
    }
}

data class UserSettings(
    val hideCompleted: Boolean,
    val notificationTime: Int,
    val visibleCategories: Set<Category>
)