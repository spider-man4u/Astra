package com.spider.astra.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.ui.chat.ChatScreen
import com.spider.astra.ui.dashboard.DashboardScreen
import com.spider.astra.ui.settings.SettingsScreen

object Screens {
    const val DASHBOARD = "dashboard"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AstraNavHost(
    repository: AssistantRepository,
    settings: AstraSettings,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screens.DASHBOARD) {
        composable(Screens.DASHBOARD) {
            DashboardScreen(navController = navController, repository = repository, settings = settings)
        }
        composable(Screens.CHAT) {
            ChatScreen(navController = navController, repository = repository)
        }
        composable(Screens.SETTINGS) {
            SettingsScreen(navController = navController, settings = settings)
        }
    }
}
