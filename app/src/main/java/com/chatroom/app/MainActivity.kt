package com.chatroom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chatroom.app.ui.navigation.NavGraph
import com.chatroom.app.ui.theme.ChatRoomTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ChatRoomApp

        setContent {
            ChatRoomTheme {
                NavGraph(
                    api = app.api,
                    socketManager = app.socketManager,
                    prefs = app.prefs,
                )
            }
        }
    }
}
