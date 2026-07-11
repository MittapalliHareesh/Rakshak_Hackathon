package com.androidblunders.rakshak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.androidblunders.rakshak.ui.theme.RakshakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Read the future message & histroric messages.
        // 1. registerListener: Captures FUTURE messages that arrive while the app is alive.
    /*    MessageExtractor.registerListener { message ->
            Log.d("RakshakPlugin", "Exposed Listener captured: ${message.content}")
        }

        // 2. getLast25Messages: Retrieves messages that were ALREADY captured in the background.
        // Note: This will be empty if the app was just installed or the process just started.
        val history = MessageExtractor.getLast25Messages()
        history.forEach { message ->
            Log.d("History", "${message.sender}: ${message.content}")
        } */

        
        
        enableEdgeToEdge()
        setContent {
            RakshakTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RakshakTheme {
        Greeting("Android")
    }
}
