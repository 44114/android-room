package com.chatroom.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chatroom.app.R
import com.chatroom.app.data.local.PreferencesManager
import com.chatroom.app.data.repository.AuthRepository
import com.chatroom.app.ui.components.TurnstileWebView
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepo: AuthRepository,
    prefs: PreferencesManager,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val context = LocalContext.current
    val siteKey by prefs.turnstileSiteKey.collectAsState(initial = "")
    val turnstileRequired by prefs.turnstileRequiredForMobile.collectAsState(initial = false)
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var turnstileToken by remember { mutableStateOf("") } // Set by WebView callback
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Pre-fetch strings for use inside coroutine callbacks
    val loginFailedText = stringResource(R.string.login_failed)
    val loginNetworkErrorText = stringResource(R.string.login_network_error)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(24.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                label = { Text(stringResource(R.string.login_username)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text(stringResource(R.string.login_password)) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "🙈" else "👁️")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            // Remember me
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text(stringResource(R.string.login_remember_me), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))

            // Turnstile WebView — shown only when server requires it for mobile
            // (default: exempted, since invite code already gates registration)
            if (turnstileRequired) {
                TurnstileWebView(
                    siteKey = siteKey,
                    onTokenReceived = { turnstileToken = it; errorMessage = null },
                    onError = { errorMessage = it },
                )
            } else {
                // Server exempts mobile from Turnstile; include a hint for transparency
                Text(
                    stringResource(R.string.login_turnstile_skipped),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                LaunchedEffect(Unit) { turnstileToken = "mobile-exempt" }
            }
            Spacer(Modifier.height(16.dp))

            // Error
            errorMessage?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            // Submit
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        authRepo.login(username, password, rememberMe, turnstileToken)
                            .onSuccess { resp ->
                                if (resp.redirect != null || resp.success == true) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = resp.error ?: loginFailedText
                                    turnstileToken = ""
                                }
                            }
                            .onFailure { errorMessage = it.message ?: loginNetworkErrorText }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.login_button))
            }
            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text(stringResource(R.string.login_no_account))
            }
        }
    }
}

