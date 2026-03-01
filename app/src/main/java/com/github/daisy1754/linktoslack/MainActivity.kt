package com.github.daisy1754.linktoslack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.daisy1754.linktoslack.ui.theme.LinktoslackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = buildSharedText(intent)
        val isShareIntent = intent.action == Intent.ACTION_SEND

        setContent {
            LinktoslackTheme {
                if (isShareIntent) {
                    ShareScreen(sharedText = sharedText, onFinish = { finish() })
                } else {
                    NotShareIntentScreen()
                }
            }
        }
    }

    private fun buildSharedText(intent: Intent): String {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        return if (!subject.isNullOrBlank() && text != subject) "$subject\n$text" else text
    }
}

@Composable
fun ShareScreen(sharedText: String, onFinish: () -> Unit) {
    val repo = remember { SlackRepository() }
    val scope = rememberCoroutineScope()

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            runCatching {
                val token = BuildConfig.SLACK_BOT_TOKEN
                val userId = BuildConfig.SLACK_RECIPIENT_USER_ID
                val channelId = repo.openDmChannel(token, userId)
                repo.sendMessage(token, channelId, sharedText)
            }.onSuccess {
                onFinish()
            }.onFailure { e ->
                errorMessage = e.message ?: "Failed to send"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(32.dp)
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun NotShareIntentScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Share a URL or text to this app to send it to Slack.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(32.dp)
        )
    }
}
