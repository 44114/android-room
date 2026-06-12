package com.chatroom.app.data.model

/** Current user info from /auth/check or login response. */
data class User(
    val id: Int = 0,
    val username: String = "",
)
