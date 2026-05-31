package com.musornibak.korvus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.musornibak.korvus.data.prefs.UserPrefs
import com.musornibak.korvus.ui.chat.ChatScreen
import com.musornibak.korvus.ui.onboarding.OnboardingScreen
import com.musornibak.korvus.ui.theme.KorvusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KorvusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val prefs = remember { UserPrefs(KorvusApp.instance) }
    val userName by prefs.userName.collectAsState(initial = null)

    when {
        userName == null -> {} // still loading
        userName!!.isBlank() -> OnboardingScreen(onDone = { name ->
            prefs.setUserName(name)
        })
        else -> ChatScreen(userName = userName!!)
    }
}
