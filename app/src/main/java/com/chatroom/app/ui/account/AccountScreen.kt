package com.chatroom.app.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.chatroom.app.R
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

    // Pre-fetch strings for use inside coroutine callbacks
    val changePasswordSuccessText = stringResource(R.string.account_change_password_success)
    val changePasswordFailedText = stringResource(R.string.account_change_password_failed)

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
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.account_back),
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
                    Text(stringResource(R.string.account_info), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.account_username_label, username))
                }
            }
            Spacer(Modifier.height(16.dp))

            // Change password
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.account_change_password), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text(stringResource(R.string.account_current_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.account_new_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPasswordConfirm,
                        onValueChange = { newPasswordConfirm = it },
                        label = { Text(stringResource(R.string.account_confirm_new_password)) },
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
                                        passwordMessage = resp.message ?: changePasswordSuccessText
                                        if (resp.success == true) {
                                            currentPassword = ""; newPassword = ""; newPasswordConfirm = ""
                                        }
                                    }
                                    .onFailure { passwordMessage = it.message ?: changePasswordFailedText }
                            }
                        },
                        enabled = currentPassword.isNotBlank() && newPassword.length >= 8 &&
                            newPassword == newPasswordConfirm,
                    ) { Text(stringResource(R.string.account_change_password)) }

                    passwordMessage?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall,
                            color = if (msg.contains(stringResource(R.string.common_success))) MaterialTheme.colorScheme.primary
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
                    Text(stringResource(R.string.account_delete_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.account_delete_warning), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text(stringResource(R.string.account_delete_password_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = deletePassword.isNotBlank(),
                    ) { Text(stringResource(R.string.account_delete_confirm)) }

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
                    Text(stringResource(R.string.account_server), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.account_current_server, serverUrl), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onChangeServer,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.account_change_server)) }
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
            ) { Text(stringResource(R.string.account_logout)) }
        }
    }
}

