package com.chatroom.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue600,
    secondary = Gray500,
    background = Gray50,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = Gray700,
    onSurface = Gray700,
    error = Red500,
    outline = Gray200,
)

@Composable
fun ChatRoomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
