package com.spider.astra.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spider.astra.ui.chat.ChatScreen
import com.spider.astra.ui.dashboard.DashboardScreen
import com.spider.astra.ui.settings.SettingsScreen

object Screens {
    const val DASHBOARD = "dashboard"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

/**
 * ASTRA navigation host.
 *
 * Phase 1 provides Dashboard (root) and Chat. Settings is a stub reachable from
 * the dashboard header; bubble/overlay screens are added in Phase 2.
 */
@Composable
fun AstraNavHost() {
    val navController = remember { rememberNavController() }

    NavHost(navController = navController, startDestination = Screens.DASHBOARD) {
        composable(Screens.DASHBOARD) { DashboardScreen(navController) }
        composable(Screens.CHAT) { ChatScreen(navController) }
        composable(Screens.SETTINGS) { SettingsScreen(navController) }
    }
}

/** Convenience to obtain the nav controller from any screen content. */
@Composable
fun rememberAstraNavController(): NavHostController =
    remember { rememberNavController() }
