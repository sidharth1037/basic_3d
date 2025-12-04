package com.example.mapview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Standard Jetpack Compose setup to make the app a full-screen Compose experience.
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // A Box is used to potentially layer UI elements over the 3D scene.
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // All the 3D and UI logic is now encapsulated in the MainScene composable.
                        MainScene()
                    }
                }
            }
        }
    }
}
