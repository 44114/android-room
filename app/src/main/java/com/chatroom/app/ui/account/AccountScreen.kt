package com.chatroom.app.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.chatroom.app.data.local.PreferencesManager
import com.chatroom.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    authRepo: AuthRepository,
    prefs: PreferencesManager,
    onLoggedOut: () -> Unit,
    onChangeServer: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val username by prefs.username.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    // Change password
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordConfirm by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf<String?>(null) }

    // Delete account
    var deletePassword by remember { mutableStateOf("") }
    var deleteMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ 账号管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // Account info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("账号信息", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("用户名：$username")
                }
            }
            Spacer(Modifier.height(16.dp))

            // Change password
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("修改密码", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("当前密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPasswordConfirm,
                        onValueChange = { newPasswordConfirm = it },
                        label = { Text("确认新密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                authRepo.changePassword(currentPassword, newPassword, newPasswordConfirm)
                                    .onSuccess { resp ->
                                        passwordMessage = resp.message ?: "密码修改成功"
                                        if (resp.success == true) {
                                            currentPassword = ""; newPassword = ""; newPasswordConfirm = ""
                                        }
                                    }
                                    .onFailure { passwordMessage = it.message ?: "修改失败" }
                            }
                        },
                        enabled = currentPassword.isNotBlank() && newPassword.length >= 8 &&
                            newPassword == newPasswordConfirm,
                    ) { Text("修改密码") }

                    passwordMessage?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall,
                            color = if (msg.contains("成功")) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Delete account
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("注销账号", style = MaterialTheme.typography.titleMedium)
                    Text("⚠️ 此操作不可撤销", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("输入密码以确认") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = deletePassword.isNotBlank(),
                    ) { Text("确认注销") }

                    deleteMessage?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Change server
            val serverUrl by prefs.serverUrl.collectAsState(initial = "")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("服务器", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("当前服务器：$serverUrl", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onChangeServer,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("更换服务器") }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Logout
            OutlinedButton(
                onClick = {
                    scope.launch {
                        authRepo.logout()
                        onLoggedOut()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("退出登录") }
        }
    }
}

