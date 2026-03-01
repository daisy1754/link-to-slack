package com.github.daisy1754.linktoslack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(sharedText: String, onFinish: () -> Unit) {
    val repo = remember { SlackRepository() }
    val scope = rememberCoroutineScope()

    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Send to Slack") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Content", style = MaterialTheme.typography.labelLarge)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = sharedText.ifBlank { "(empty)" },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    isSending = true
                    errorMessage = null
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
                            isSending = false
                        }
                    }
                },
                enabled = !isSending && sharedText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send")
                }
            }
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
