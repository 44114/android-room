package com.chatroom.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chatroom.app.data.api.ApiService
import com.chatroom.app.data.api.SocketManager
import com.chatroom.app.data.local.PreferencesManager
import com.chatroom.app.data.repository.AuthRepository
import com.chatroom.app.data.repository.ChatRepository
import com.chatroom.app.data.repository.FileRepository
import com.chatroom.app.ui.account.AccountScreen
import com.chatroom.app.ui.auth.LoginScreen
import com.chatroom.app.ui.auth.RegisterScreen
import com.chatroom.app.ui.chat.ChatScreen
import com.chatroom.app.ui.setup.SetupScreen

object Routes {
    const val SETUP = "setup"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHAT = "chat"
    const val ACCOUNT = "account"
}

@Composable
fun NavGraph(
    api: ApiService,
    socketManager: SocketManager,
    prefs: PreferencesManager,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val isServerConfigured by prefs.isServerConfigured.collectAsState(initial = false)
    val isLoggedIn by prefs.isLoggedIn.collectAsState(initial = false)

    val authRepo = AuthRepository(api, prefs)
    val chatRepo = ChatRepository(socketManager)
    val fileRepo = FileRepository(api, context)

    // Start destination: Setup if not configured, else chat if logged in, else login
    val startDest = when {
        !isServerConfigured -> Routes.SETUP
        isLoggedIn -> Routes.CHAT
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDest) {

        composable(Routes.SETUP) {
            SetupScreen(
                prefs = prefs,
                api = api,
                onConfigured = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                authRepo = authRepo,
                prefs = prefs,
                onLoginSuccess = {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                authRepo = authRepo,
                prefs = prefs,
                onRegisterSuccess = {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.CHAT) {
            ChatScreen(
                chatRepo = chatRepo,
                fileRepo = fileRepo,
                prefs = prefs,
                onNavigateToAccount = {
                    navController.navigate(Routes.ACCOUNT)
                },
            )
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(
                authRepo = authRepo,
                prefs = prefs,
                onLoggedOut = {
                    chatRepo.disconnect()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChangeServer = {
                    chatRepo.disconnect()
                    navController.navigate(Routes.SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
