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
fun RegisterScreen(
    authRepo: AuthRepository,
    prefs: PreferencesManager,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val siteKey by prefs.turnstileSiteKey.collectAsState(initial = "")
    val turnstileRequired by prefs.turnstileRequiredForMobile.collectAsState(initial = false)
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var turnstileToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Pre-fetch strings for use inside coroutine callbacks
    val registerFailedText = stringResource(R.string.register_failed)
    val networkErrorText = stringResource(R.string.login_network_error)

    // Password strength
    val passwordStrength = remember(password) {
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        score
    }

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
            Text(stringResource(R.string.register_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.register_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(24.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.take(30).filter { c -> c.isLetterOrDigit() || c == '_' }; errorMessage = null },
                label = { Text(stringResource(R.string.register_username_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(128); errorMessage = null },
                label = { Text(stringResource(R.string.register_password_hint)) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "🙈" else "👁️")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            // Strength indicator
            if (password.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { passwordStrength / 4f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                Text(
                    when {
                        passwordStrength <= 1 -> stringResource(R.string.register_password_strength_weak)
                        passwordStrength == 2 -> stringResource(R.string.register_password_strength_fair)
                        passwordStrength == 3 -> stringResource(R.string.register_password_strength_good)
                        else -> stringResource(R.string.register_password_strength_strong)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Confirm password
            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it; errorMessage = null },
                label = { Text(stringResource(R.string.register_confirm_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordConfirm.isNotEmpty() && password != passwordConfirm,
                supportingText = if (passwordConfirm.isNotEmpty() && password != passwordConfirm) {
                    { Text(stringResource(R.string.register_password_mismatch)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            // Invite code
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it; errorMessage = null },
                label = { Text(stringResource(R.string.register_invite_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            // Turnstile WebView — shown only when server requires it for mobile
            if (turnstileRequired) {
                TurnstileWebView(
                    siteKey = siteKey,
                    onTokenReceived = { turnstileToken = it; errorMessage = null },
                    onError = { errorMessage = it },
                )
            } else {
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
                        authRepo.register(username, password, passwordConfirm, inviteCode, turnstileToken)
                            .onSuccess { resp ->
                                if (resp.redirect != null || resp.success == true) {
                                    onRegisterSuccess()
                                } else {
                                    errorMessage = resp.error ?: registerFailedText
                                    turnstileToken = ""
                                }
                            }
                            .onFailure { errorMessage = it.message ?: networkErrorText }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && username.length >= 3 && password.length >= 8 &&
                    password == passwordConfirm && inviteCode.isNotBlank(),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.register_button))
            }
            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(stringResource(R.string.register_has_account))
            }
        }
    }
}
