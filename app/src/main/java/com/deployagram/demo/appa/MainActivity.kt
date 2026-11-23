package com.deployagram.demo.appa

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The one screen of AppA: enter an email, press "Recommend book", see the suggestion. */
class MainActivity : ComponentActivity() {

    private val bookOverflow = BookOverflowClient()
    private val aliceMessager = AliceMessagerClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RecommendBookScreen(bookOverflow, aliceMessager) }
    }
}

/** The one screen of AppA. "Load Config" calls AliceMessager, sending "Hello" once a book has been recommended. */
@Composable
fun RecommendBookScreen(bookOverflow: BookOverflowClient, aliceMessager: AliceMessagerClient = AliceMessagerClient()) {
    var email by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var recommended by remember { mutableStateOf(false) }
    var configResult by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(48.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().testTag("email"),
        )
        Button(onClick = {
            val address = email.trim()
            result = "Asking BookOverflow…"
            bookOverflow.suggestionFor(address) { outcome ->
                // The answer arrives on a background thread, so the screen is updated on the main thread.
                Handler(Looper.getMainLooper()).post {
                    outcome.onSuccess { recommended = true }
                    result = outcome.fold(
                        onSuccess = { it.describe() },
                        onFailure = { failure -> messageFor(address, failure) },
                    )
                }
            }
        }) {
            Text("Recommend book")
        }
        Text(result, fontSize = 18.sp, modifier = Modifier.testTag("result"))
        Button(onClick = {
            // "Hello" once a book has been recommended, and an empty payload before that.
            val payload = if (recommended) "Hello" else null
            configResult = "Asking AliceMessager…"
            aliceMessager.sendConfig(payload) { outcome ->
                Handler(Looper.getMainLooper()).post {
                    configResult = outcome.fold(
                        onSuccess = { it },
                        onFailure = { failure ->
                            if (failure is AliceMessagerRefused) failure.reply else "Sorry, ${failure.message}"
                        },
                    )
                }
            }
        }) {
            Text("Load Config")
        }
        Text(configResult, fontSize = 18.sp, modifier = Modifier.testTag("config"))
    }
}

private fun messageFor(email: String, failure: Throwable): String =
    if (email.isEmpty() && failure is BookOverflowRefused && failure.statusCode == 400) {
        "You must enter a valid email!"
    } else {
        "Sorry, no suggestion right now (${failure.message})"
    }
