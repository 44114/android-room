package com.chatroom.app.data.api

import android.util.Log
import com.chatroom.app.BuildConfig
import com.chatroom.app.data.local.PreferencesManager
import com.chatroom.app.data.model.Message
import com.chatroom.app.data.model.MessageType
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * WebSocket manager using Socket.IO Java client.
 *
 * Emits:
 *   messages  — new chat messages (text, file, system)
 *   connected — true/false connection state
 *   typing    — Pair<username, isTyping>
 */
class SocketManager(private val prefs: PreferencesManager) {

    private val serverUrl: String
        get() = runBlocking { prefs.getServerUrlOnce() }
            .ifBlank { BuildConfig.BASE_URL }

    private var socket: Socket? = null

    private val _messages = Channel<Message>(Channel.BUFFERED)
    val messages: Flow<Message> = _messages.receiveAsFlow()

    private val _connected = Channel<Boolean>(Channel.CONFLATED)
    val connected: Flow<Boolean> = _connected.receiveAsFlow()

    private val _typing = Channel<Pair<String, Boolean>>(Channel.CONFLATED)
    val typing: Flow<Pair<String, Boolean>> = _typing.receiveAsFlow()

    fun connect() {
        if (socket?.connected() == true) return

        val options = IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000
            reconnectionDelayMax = 30000
            timeout = 20000
        }

        socket = IO.socket(serverUrl, options).apply {

            on(Socket.EVENT_CONNECT) {
                Log.i(TAG, "WebSocket connected")
                _connected.trySend(true)
            }

            on(Socket.EVENT_DISCONNECT) {
                Log.w(TAG, "WebSocket disconnected")
                _connected.trySend(false)
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "WebSocket connect error: ${args.firstOrNull()}")
            }

            on("new_message") { args ->
                try {
                    val data = JSONObject(args[0].toString())
                    val msg = Message(
                        id = data.optLong("id", 0),
                        type = MessageType.fromString(data.optString("type", "text")),
                        senderId = data.optInt("sender_id", 0),
                        username = data.optString("username", ""),
                        content = data.optString("content", ""),
                        fileId = if (data.isNull("file_id")) null else data.optInt("file_id"),
                        filename = data.optString("filename", null),
                        fileSize = if (data.isNull("file_size")) null else data.optLong("file_size", 0),
                        timestamp = data.optString("timestamp", ""),
                    )
                    _messages.trySend(msg)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message: ${args.firstOrNull()}", e)
                }
            }

            on("system_message") { args ->
                try {
                    val data = JSONObject(args[0].toString())
                    val msg = Message(
                        type = MessageType.SYSTEM,
                        username = data.optString("username", ""),
                        content = data.optString("message", ""),
                        timestamp = data.optString("timestamp", ""),
                    )
                    _messages.trySend(msg)
                } catch (_: Exception) {}
            }

            on("user_typing") { args ->
                try {
                    val data = JSONObject(args[0].toString())
                    _typing.trySend(
                        data.optString("username", "") to data.optBoolean("typing", false)
                    )
                } catch (_: Exception) {}
            }

            on("error") { args ->
                Log.e(TAG, "Server error: ${args.firstOrNull()}")
            }

            connect()
        }
    }

    fun sendMessage(content: String) {
        socket?.emit("send_message", JSONObject().apply {
            put("content", content)
        })
    }

    fun sendFileMessage(fileId: Int, filename: String, fileSize: Long) {
        socket?.emit("send_file_message", JSONObject().apply {
            put("file_id", fileId)
            put("filename", filename)
            put("file_size", fileSize)
        })
    }

    fun sendTypingIndicator(isTyping: Boolean) {
        socket?.emit("typing", JSONObject().apply {
            put("typing", isTyping)
        })
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connected.trySend(false)
    }

    companion object {
        private const val TAG = "SocketManager"
    }
}
