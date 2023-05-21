package com.poe.poegps.feature.data.remote.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.poe.poegps.app.database.DatabaseModule.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FilialManager(private val context: Context) {

    companion object {
        private val FILIAL_KEY = stringPreferencesKey("filial")
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