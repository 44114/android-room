package com.chatroom.app.data.model

/** Chat message received via WebSocket or stored locally. */
data class Message(
    val id: Long = 0,
    val type: MessageType = MessageType.TEXT,
    val senderId: Int = 0,
    val username: String = "",
    val content: String = "",
    val fileId: Int? = null,
    val filename: String? = null,
    val fileSize: Long? = null,
    val timestamp: String = "",
    val isLocal: Boolean = false,   // optimistic UI — not yet confirmed by server
)

enum class MessageType {
    TEXT,
    FILE,
    SYSTEM,
    ;

    companion object {
        fun fromString(value: String): MessageType = when (value.lowercase()) {
            "text" -> TEXT
            "file" -> FILE
            "system", "join", "leave" -> SYSTEM
            else -> TEXT
        }
    }
}
