package com.chatroom.app.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chatroom.app.data.api.ApiService
import com.chatroom.app.data.local.PreferencesManager
import kotlinx.coroutines.launch

/**
 * First-launch or settings screen where the user enters their server URL.
 *
 * The URL should be something like:
 *   http://192.168.1.100:9888
 *   https://chat.example.com
 */
@Composable
fun SetupScreen(
    prefs: PreferencesManager,
    api: ApiService,
    onConfigured: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val savedUrl by prefs.serverUrl.collectAsState(initial = "")

    var url by remember(savedUrl) { mutableStateOf(savedUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(true) }

    // If we navigate here from AccountScreen, there may already be a saved URL
    val isFirstSetup = savedUrl.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💬", style = MaterialTheme.typography.displayLarge)

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (isFirstSetup) "欢迎使用聊天室" else "服务器设置",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (isFirstSetup) "请输入聊天室服务器的地址" else "修改服务器地址后需重新登录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(Modifier.height(32.dp))

        // URL input
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                errorMessage = null
            },
            label = { Text("服务器地址") },
            placeholder = { Text("http://192.168.1.100:9888") },
            singleLine = true,
            isError = errorMessage != null,
            supportingText = errorMessage?.let { msg -> { Text(msg) } } ?: if (showHint) {
                { Text("请输入完整 URL，包括 http:// 和端口号") }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    val trimmedUrl = url.trim().trimEnd('/')
                    if (trimmedUrl.isBlank()) {
                        errorMessage = "请输入服务器地址"
                        isSaving = false
                        return@launch
                    }
                    if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                        errorMessage = "地址必须以 http:// 或 https:// 开头"
                        isSaving = false
                        return@launch
                    }
                    errorMessage = null
                    showHint = false
                    prefs.setServerUrl(trimmedUrl)

                    // Fetch server config (Turnstile site key, limits, etc.)
                    try {
                        val config = api.fetchConfig()
                        prefs.setTurnstileSiteKey(config.turnstileSiteKey)
                        prefs.setTurnstileRequiredForMobile(config.turnstileRequiredForMobile)
                    } catch (e: Exception) {
                        // Config fetch failed — still allow navigation but
                        // Turnstile won't work until the user retries.
                        errorMessage = "无法获取服务器配置: ${e.message}"
                        isSaving = false
                        return@launch
                    }

                    isSaving = false
                    onConfigured()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && url.isNotBlank(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(if (isFirstSetup) "连接服务器" else "保存并重新连接")
            }
        }

        if (isFirstSetup) {
            Spacer(Modifier.height(16.dp))

            // Quick-set button for emulator
            OutlinedButton(
                onClick = {
                    url = "http://10.0.2.2:9888"
                    showHint = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("使用模拟器默认地址 (10.0.2.2:9888)")
            }
        }
    }
}
