package com.poe.poegps.feature.data.remote.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.poe.poegps.app.database.DatabaseModule.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FilialManager(private val context: Context) {

    companion object {
        private val FILIAL_KEY = stringPreferencesKey("filial")
        private val DOWNLOADED_AT_KEY = longPreferencesKey("downloaded_at")
    }

    /** When the branch's objects were last downloaded completely (epoch ms); kept on the phone only. */
    fun getDownloadedAt(): Flow<Long?> = context.dataStore.data.map { it[DOWNLOADED_AT_KEY] }

    suspend fun saveDownloadedAt(timeMs: Long) {
        context.dataStore.edit { it[DOWNLOADED_AT_KEY] = timeMs }
    }

    fun getFilial() : Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[FILIAL_KEY]
        }
    }

    suspend fun saveFilial(filial: String) {
        context.dataStore.edit { preferences ->
            preferences[FILIAL_KEY] = filial
        }
    }

    suspend fun deleteFilial() {
        context.dataStore.edit { preferences ->
            preferences.remove(FILIAL_KEY)
        }
    }
}