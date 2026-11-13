package com.spider.astra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.spider.astra.assistant.core.MockAssistantRepository
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.navigation.AstraNavHost
import com.spider.astra.ui.theme.AstraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Main entry point for ASTRA.
 *
 * Keeps the activity thin: it only wires up the assistant repository, the settings
 * store (DataStore), and the Compose navigation host. All business logic lives in
 * the assistant module; all UI lives in the ui module.
 */
class MainActivity : ComponentActivity() {

    private val repository: com.spider.astra.assistant.core.AssistantRepository by lazy {
        // The UI thread dispatcher is correct here because the mock repository only
        // updates state and schedules UI work. Real backend I/O (Phase 5) will use
        // Dispatchers.Default/IO and a dedicated repository scope.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        MockAssistantRepository(scope)
    }

    private val settings: AstraSettings by lazy {
        AstraSettings.create(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstraTheme {
                AstraNavHost()
            }
        }
    }
}
