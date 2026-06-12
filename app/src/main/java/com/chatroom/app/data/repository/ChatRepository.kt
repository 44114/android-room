package com.chatroom.app.data.repository

import com.chatroom.app.data.api.SocketManager
import com.chatroom.app.data.model.Message
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val socketManager: SocketManager,
) {
    val messages: Flow<Message> = socketManager.messages
    val connected: Flow<Boolean> = socketManager.connected
    val typing: Flow<Pair<String, Boolean>> = socketManager.typing

    fun connect() = socketManager.connect()
    fun disconnect() = socketManager.disconnect()

    fun sendMessage(content: String) {
        socketManager.sendMessage(content)
    }

    fun sendFileMessage(fileId: Int, filename: String, fileSize: Long) {
        socketManager.sendFileMessage(fileId, filename, fileSize)
    }

    fun sendTypingIndicator(isTyping: Boolean) {
        socketManager.sendTypingIndicator(isTyping)
    }
}
