package com.chatroom.app

import android.app.Application
import com.chatroom.app.data.api.ApiService
import com.chatroom.app.data.api.SocketManager
import com.chatroom.app.data.local.PreferencesManager

/** Application class — holds singletons for the dependency graph. */
class ChatRoomApp : Application() {

    lateinit var api: ApiService
        private set
    lateinit var socketManager: SocketManager
        private set
    lateinit var prefs: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesManager(this)
        api = ApiService(prefs)
        socketManager = SocketManager(prefs)
    }

    companion object {
        lateinit var instance: ChatRoomApp
            private set
    }
}
