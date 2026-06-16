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
import androidx.compose.ui.res.stringResource
import com.chatroom.app.R

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

    // Pre-fetch strings for use inside coroutine callbacks
    val urlEmptyErrorText = stringResource(R.string.setup_url_empty_error)
    val urlInvalidErrorText = stringResource(R.string.setup_url_invalid_error)
    val configErrorFormat = stringResource(R.string.setup_config_error)
    val unknownErrorText = stringResource(R.string.turnstile_unknown_error)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.setup_emoji), style = MaterialTheme.typography.displayLarge)

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (isFirstSetup) stringResource(R.string.setup_welcome) else stringResource(R.string.setup_server_settings),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (isFirstSetup) stringResource(R.string.setup_welcome_hint) else stringResource(R.string.setup_reconnect_hint),
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
            label = { Text(stringResource(R.string.setup_server_url)) },
            placeholder = { Text(stringResource(R.string.setup_url_placeholder)) },
            singleLine = true,
            isError = errorMessage != null,
            supportingText = errorMessage?.let { msg -> { Text(msg) } } ?: if (showHint) {
                { Text(stringResource(R.string.setup_url_hint)) }
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
                        errorMessage = urlEmptyErrorText
                        isSaving = false
                        return@launch
                    }
                    if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                        errorMessage = urlInvalidErrorText
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
                        errorMessage = String.format(configErrorFormat, e.message ?: unknownErrorText)
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
                Text(if (isFirstSetup) stringResource(R.string.setup_connect_button) else stringResource(R.string.setup_reconnect_button))
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
                Text(stringResource(R.string.setup_emulator_button))
            }
        }
    }
}
