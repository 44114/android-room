package com.chatroom.app.ui.components

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

/*
 * Registers text fields with the platform autofill framework so password
 * managers (Google Password Manager, Bitwarden, KeePassDX, …) can fill them and
 * offer to save the credentials afterwards.
 *
 * Compose has no stable autofill modifier at the version this project uses
 * (compose-bom 2024.10.01 → ui 1.7.5); the declarative `ContentType` semantics
 * only arrive in 1.8. The experimental API is wrapped here so the opt-in stays
 * in this file and the screens keep calling plain, stable-looking modifiers.
 */

/** Existing account name — for the login screen. */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofillUsername(onFill: (String) -> Unit): Modifier =
    autofillFor(AutofillTypes.username, onFill)

/** Existing password — for the login screen. */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofillPassword(onFill: (String) -> Unit): Modifier =
    autofillFor(AutofillTypes.password, onFill)

/** Account name being chosen — for the registration screen. */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofillNewUsername(onFill: (String) -> Unit): Modifier =
    autofillFor(AutofillTypes.newUsername, onFill)

/** Password being chosen — lets a manager offer to generate and save one. */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofillNewPassword(onFill: (String) -> Unit): Modifier =
    autofillFor(AutofillTypes.newPassword, onFill)

@OptIn(ExperimentalComposeUiApi::class)
private object AutofillTypes {
    val username = listOf(AutofillType.Username)
    val password = listOf(AutofillType.Password)
    val newUsername = listOf(AutofillType.NewUsername)
    val newPassword = listOf(AutofillType.NewPassword)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.autofillFor(
    types: List<AutofillType>,
    onFill: (String) -> Unit,
): Modifier = composed {
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current

    // Keep the latest lambda without rebuilding the node on every recomposition.
    val currentOnFill by rememberUpdatedState(onFill)
    val node = remember(types) {
        AutofillNode(autofillTypes = types, onFill = { currentOnFill(it) })
    }

    DisposableEffect(node) {
        autofillTree += node
        onDispose { autofillTree.children.remove(node.id) }
    }

    this
        .onGloballyPositioned { node.boundingBox = it.boundsInWindow() }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) requestAutofillForNode(node)
                else cancelAutofillForNode(node)
            }
        }
}
