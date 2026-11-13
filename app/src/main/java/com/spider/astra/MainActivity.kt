package com.spider.astra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.spider.astra.assistant.core.AstraAssistantRepository
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.navigation.AstraNavHost
import com.spider.astra.ui.theme.AstraTheme

class MainActivity : ComponentActivity() {
    private val repository by lazy { AstraAssistantRepository(applicationContext) }
    private val settings by lazy { AstraSettings.create(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = "system")
            AstraTheme(themeMode = themeMode) {
                AstraNavHost(repository = repository, settings = settings)
            }
        }
    }
}
