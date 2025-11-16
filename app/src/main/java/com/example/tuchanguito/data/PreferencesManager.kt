package com.example.tuchanguito.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    object Keys {
        val THEME = stringPreferencesKey("theme") // system | light | dark
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val PENDING_EMAIL = stringPreferencesKey("pending_email")
        val PENDING_PASSWORD = stringPreferencesKey("pending_password")
        val CURRENT_PANTRY_ID = stringPreferencesKey("current_pantry_id")
        val LAST_OPENED_LIST_ID = stringPreferencesKey("last_opened_list_id")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMEMBER_ME] ?: true }
    val currentUserId: Flow<Long?> = context.dataStore.data.map { it[Keys.CURRENT_USER_ID]?.toLongOrNull() }
    val authToken: Flow<String?> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] }
    val pendingEmail: Flow<String?> = context.dataStore.data.map { it[Keys.PENDING_EMAIL] }
    val pendingPassword: Flow<String?> = context.dataStore.data.map { it[Keys.PENDING_PASSWORD] }
    val currentPantryId: Flow<Long?> = context.dataStore.data.map { it[Keys.CURRENT_PANTRY_ID]?.toLongOrNull() }
    val lastOpenedListId: Flow<Long?> = context.dataStore.data.map { it[Keys.LAST_OPENED_LIST_ID]?.toLongOrNull() }

    suspend fun setTheme(value: String) { context.dataStore.edit { it[Keys.THEME] = value } }
    suspend fun setRememberMe(value: Boolean) { context.dataStore.edit { it[Keys.REMEMBER_ME] = value } }
    suspend fun setCurrentUserId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id == null) {
                prefs.remove(Keys.CURRENT_USER_ID)
            } else {
                prefs[Keys.CURRENT_USER_ID] = id.toString()
                val defaultPantry = prefs[Keys.CURRENT_PANTRY_ID]
                if (defaultPantry != null) {
                    val userKey = pantryKeyForUser(id)
                    if (!prefs.contains(userKey)) {
                        prefs[userKey] = defaultPantry
                    }
                }
            }
        }
    }
    suspend fun setAuthToken(token: String?) { context.dataStore.edit { if (token == null) it.remove(Keys.AUTH_TOKEN) else it[Keys.AUTH_TOKEN] = token } }
    suspend fun setPendingCredentials(email: String, password: String) { context.dataStore.edit { it[Keys.PENDING_EMAIL] = email; it[Keys.PENDING_PASSWORD] = password } }
    suspend fun clearPendingCredentials() { context.dataStore.edit { it.remove(Keys.PENDING_EMAIL); it.remove(Keys.PENDING_PASSWORD) } }
    suspend fun setCurrentPantryId(id: Long?) { setCurrentPantryIdForUser(userId = null, id = id) }
    suspend fun setCurrentPantryIdForUser(userId: Long?, id: Long?) {
        val key = pantryKeyForUser(userId)
        context.dataStore.edit {
            if (id == null) {
                it.remove(key)
                if (userId == null) it.remove(Keys.CURRENT_PANTRY_ID)
            } else {
                it[key] = id.toString()
                if (userId == null) it[Keys.CURRENT_PANTRY_ID] = id.toString()
            }
        }
    }
    suspend fun getCurrentPantryIdForUser(userId: Long?): Long? {
        val key = pantryKeyForUser(userId)
        return context.dataStore.data.map { prefs ->
            prefs[key]?.toLongOrNull() ?: prefs[Keys.CURRENT_PANTRY_ID]?.toLongOrNull()
        }.first()
    }
    suspend fun setLastOpenedListId(id: Long?) { context.dataStore.edit { if (id == null) it.remove(Keys.LAST_OPENED_LIST_ID) else it[Keys.LAST_OPENED_LIST_ID] = id.toString() } }

    private fun pantryKeyForUser(userId: Long?): Preferences.Key<String> =
        stringPreferencesKey("current_pantry_id_${userId ?: "default"}")
}
