package com.spider.astra.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spider.astra.R
import com.spider.astra.navigation.Screens
import com.spider.astra.ui.theme.AstraTheme
import androidx.navigation.NavHostController
import android.content.Intent
import android.provider.Settings

/**
 * Settings screen (Phase 1 stub).
 *
 * Currently exposes the overlay-permission entry point required for the floating
 * bubble (Phase 2) and a placeholder for assistant preferences. No backend
 * credentials are stored or displayed here.
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    context: android.content.Context = LocalContext.current
) {
    AstraTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings)) },
                colors = androidx.compose.material3.TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFF050A14)
                ),
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            imageVector = ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ASTRA Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    openOverlayPermissionSettings(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.grant_overlay))
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate(Screens.DASHBOARD) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Back to dashboard")
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Backend: mock (Phase 1)\nNo API keys stored.\nVoice: coming in Phase 4.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun openOverlayPermissionSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        .setData(android.net.Uri.parse("package:${context.packageName}"))
    context.startActivity(intent)
}
