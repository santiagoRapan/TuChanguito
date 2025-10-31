package com.example.tuchanguito.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    object Keys {
        val THEME = stringPreferencesKey("theme") // system | light | dark
        val CURRENCY = stringPreferencesKey("currency") // e.g., $ or ARS$
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val PENDING_EMAIL = stringPreferencesKey("pending_email")
        val PENDING_PASSWORD = stringPreferencesKey("pending_password")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val currency: Flow<String> = context.dataStore.data.map { it[Keys.CURRENCY] ?: "$" }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMEMBER_ME] ?: true }
    val currentUserId: Flow<Long?> = context.dataStore.data.map { it[Keys.CURRENT_USER_ID]?.toLongOrNull() }
    val authToken: Flow<String?> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] }
    val pendingEmail: Flow<String?> = context.dataStore.data.map { it[Keys.PENDING_EMAIL] }
    val pendingPassword: Flow<String?> = context.dataStore.data.map { it[Keys.PENDING_PASSWORD] }

    suspend fun setTheme(value: String) { context.dataStore.edit { it[Keys.THEME] = value } }
    suspend fun setCurrency(value: String) { context.dataStore.edit { it[Keys.CURRENCY] = value } }
    suspend fun setRememberMe(value: Boolean) { context.dataStore.edit { it[Keys.REMEMBER_ME] = value } }
    suspend fun setCurrentUserId(id: Long?) { context.dataStore.edit { if (id == null) it.remove(Keys.CURRENT_USER_ID) else it[Keys.CURRENT_USER_ID] = id.toString() } }
    suspend fun setAuthToken(token: String?) { context.dataStore.edit { if (token == null) it.remove(Keys.AUTH_TOKEN) else it[Keys.AUTH_TOKEN] = token } }
    suspend fun setPendingCredentials(email: String, password: String) { context.dataStore.edit { it[Keys.PENDING_EMAIL] = email; it[Keys.PENDING_PASSWORD] = password } }
    suspend fun clearPendingCredentials() { context.dataStore.edit { it.remove(Keys.PENDING_EMAIL); it.remove(Keys.PENDING_PASSWORD) } }
}
