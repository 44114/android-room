package com.chatroom.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chatroom_prefs")

/**
 * Lightweight preferences backed by DataStore.
 */
class PreferencesManager(private val context: Context) {

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: ""
    }

    val userId: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID] ?: 0
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: ""
    }

    val isServerConfigured: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_SERVER_URL].isNullOrBlank()
    }

    val turnstileSiteKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TURNSTILE_SITE_KEY] ?: ""
    }

    val turnstileRequiredForMobile: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TURNSTILE_REQUIRED_FOR_MOBILE] ?: false
    }

    val csrfToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CSRF_TOKEN] ?: ""
    }

    /** Read the current server URL. */
    suspend fun getServerUrlOnce(): String {
        var value = ""
        context.dataStore.data.take(1).collect { prefs ->
            value = prefs[KEY_SERVER_URL] ?: ""
        }
        return value
    }

    suspend fun setLoggedIn(userId: Int, username: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun setLoggedOut() {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = false
            prefs[KEY_USER_ID] = 0
            prefs[KEY_USERNAME] = ""
            prefs[KEY_CSRF_TOKEN] = ""
        }
    }

    suspend fun setCsrfToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CSRF_TOKEN] = token
        }
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = url
        }
    }

    suspend fun setTurnstileSiteKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TURNSTILE_SITE_KEY] = key
        }
    }

    suspend fun setTurnstileRequiredForMobile(required: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TURNSTILE_REQUIRED_FOR_MOBILE] = required
        }
    }

    companion object {
        private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_TURNSTILE_SITE_KEY = stringPreferencesKey("turnstile_site_key")
        private val KEY_TURNSTILE_REQUIRED_FOR_MOBILE = booleanPreferencesKey("turnstile_required_for_mobile")
        private val KEY_CSRF_TOKEN = stringPreferencesKey("csrf_token")
    }
}
