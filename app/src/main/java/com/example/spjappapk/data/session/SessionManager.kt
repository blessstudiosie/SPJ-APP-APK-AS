package com.example.spjappapk.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.spjappapk.ui.auth.SalesPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {
    private val USER_KEY = stringPreferencesKey("user_data")
    private val json = Json { ignoreUnknownKeys = true }

    val userFlow: Flow<SalesPerson?> = context.dataStore.data.map { preferences ->
        preferences[USER_KEY]?.let { userJson ->
            try {
                json.decodeFromString<SalesPerson>(userJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveSession(user: SalesPerson) {
        context.dataStore.edit { preferences ->
            preferences[USER_KEY] = json.encodeToString(user)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_KEY)
        }
    }
}
