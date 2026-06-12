package com.chatroom.app.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chatroom.app.data.api.SocketManager
import com.chatroom.app.data.local.PreferencesManager
import com.chatroom.app.data.model.Message
import com.chatroom.app.data.model.MessageType
import com.chatroom.app.data.repository.ChatRepository
import com.chatroom.app.data.repository.FileRepository
import com.chatroom.app.ui.components.FileCard
import com.chatroom.app.ui.components.MessageBubble
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatRepo: ChatRepository,
    fileRepo: FileRepository,
    prefs: PreferencesManager,
    onNavigateToAccount: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val username by prefs.username.collectAsState(initial = "")
    val connected by chatRepo.connected.collectAsState(initial = false)

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    var typingUser by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf<Pair<Long, Long>?>(null) } // (uploaded, total)
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // Connect WebSocket
    LaunchedEffect(Unit) {
        chatRepo.connect()
        // Auto-scroll on new messages
        chatRepo.messages.collect { msg ->
            messages = messages + msg
            if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == messages.size - 2 ||
                messages.size <= 3) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        chatRepo.typing.collect { (user, isTyping) ->
            typingUser = if (isTyping && user != username) user else ""
        }
    }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                fileRepo.uploadFile(it) { uploaded, total ->
                    uploadProgress = uploaded to total
                }.onSuccess { result ->
                    chatRepo.sendFileMessage(result.fileId!!, result.filename!!, result.fileSize)
                    uploadProgress = null
                }.onFailure { err ->
                    snackbarMessage = err.message ?: "上传失败"
                    uploadProgress = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("# 大厅") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    // Connection indicator
                    Icon(
                        imageVector = if (connected) Icons.Filled.Circle else Icons.Filled.Circle,
                        contentDescription = if (connected) "已连接" else "未连接",
                        tint = if (connected) androidx.compose.ui.graphics.Color(0xFF27AE60)
                        else androidx.compose.ui.graphics.Color(0xFFE74C3C),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (connected) "已连接" else "断开",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    // Account button
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(Icons.Filled.Person, contentDescription = "账号")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                state = listState,
                reverseLayout = false,
            ) {
                items(messages, key = { it.id.takeIf { id -> id > 0 } ?: it.hashCode() }) { msg ->
                    when (msg.type) {
                        MessageType.SYSTEM -> {
                            Text(
                                msg.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                        MessageType.FILE -> {
                            FileCard(message = msg, onDownload = { fileId ->
                                // Open download URL in browser or download manager
                                val url = fileRepo.getDownloadUrl(fileId)
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse(url)
                                }
                                context.startActivity(intent)
                            })
                        }
                        else -> {
                            MessageBubble(
                                message = msg,
                                isMine = msg.username == username,
                            )
                        }
                    }
                }
            }

            // Typing indicator
            if (typingUser.isNotEmpty()) {
                Text(
                    "$typingUser 正在输入...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Upload progress
            uploadProgress?.let { (uploaded, total) ->
                LinearProgressIndicator(
                    progress = { uploaded.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Text(
                    "上传中: ${uploaded / 1024 / 1024}MB / ${total / 1024 / 1024}MB",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // File attach
                IconButton(onClick = { filePicker.launch("*/*") }) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "发送文件")
                }

                // Text input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        chatRepo.sendTypingIndicator(it.isNotEmpty())
                    },
                    placeholder = { Text("输入消息...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                )

                // Send
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            chatRepo.sendMessage(inputText.trim())
                            inputText = ""
                            chatRepo.sendTypingIndicator(false)
                        }
                    },
                    enabled = inputText.isNotBlank() && connected,
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "发送")
                }
            }
        }
    }
}
