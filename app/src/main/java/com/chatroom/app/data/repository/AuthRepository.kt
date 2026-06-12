package com.chatroom.app.data.repository

import com.chatroom.app.data.api.ApiService
import com.chatroom.app.data.local.PreferencesManager
import com.chatroom.app.data.model.ApiResponse
import com.chatroom.app.data.model.AuthStatus

class AuthRepository(
    private val api: ApiService,
    private val prefs: PreferencesManager,
) {
    suspend fun register(
        username: String,
        password: String,
        passwordConfirm: String,
        inviteCode: String,
        turnstileToken: String,
    ): Result<ApiResponse> = runCatching {
        val resp = api.register(username, password, passwordConfirm, inviteCode, turnstileToken)
        if (resp.success == true || resp.redirect != null) {
            prefs.setLoggedIn(0, username)
        }
        resp
    }

    suspend fun login(
        username: String,
        password: String,
        rememberMe: Boolean,
        turnstileToken: String,
    ): Result<ApiResponse> = runCatching {
        val resp = api.login(username, password, rememberMe, turnstileToken)
        if (resp.success == true || resp.redirect != null) {
            // Check auth to get the real user ID
            val status = api.checkAuth()
            if (status.loggedIn) {
                prefs.setLoggedIn(status.userId ?: 0, status.username ?: username)
            }
        }
        resp
    }

    suspend fun logout(): Result<ApiResponse> = runCatching {
        val resp = api.logout()
        prefs.setLoggedOut()
        resp
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        newPasswordConfirm: String,
    ): Result<ApiResponse> = runCatching {
        api.changePassword(currentPassword, newPassword, newPasswordConfirm)
    }

    suspend fun deleteAccount(password: String): Result<ApiResponse> = runCatching {
        val resp = api.deleteAccount(password)
        prefs.setLoggedOut()
        resp
    }

    suspend fun checkAuth(): AuthStatus = api.checkAuth()
}
