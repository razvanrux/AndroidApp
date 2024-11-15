package com.rux.first_test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rux.first_test.ui.theme.SecondEverTestTheme

class SecondActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecondEverTestTheme { // Apply the theme
                // Retrieve the data passed from the first activity
                val text = intent.getStringExtra("EXTRA_TEXT") ?: "No text received"
                SecondContent(text) // Pass the text to the composable
            }
        }
    }

    @Composable
    fun SecondContent(text: String) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background // Use the background color from the theme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onBackground, // Text color based on theme
                    modifier = Modifier.padding(24.dp) // Add padding to the text
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewSecondContent() {
        SecondEverTestTheme {
            SecondContent("Preview text")
        }
    }
}
