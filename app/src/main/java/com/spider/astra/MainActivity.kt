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

class MainActivity : ComponentActivity() {
    private val repository by lazy {
        MockAssistantRepository(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    }
    private val settings by lazy { AstraSettings.create(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstraTheme {
                AstraNavHost(repository = repository, settings = settings)
            }
        }
    }
}