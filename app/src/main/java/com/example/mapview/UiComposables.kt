package com.example.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Provides directional buttons that emit move events.
 * This is a highly performant and reusable pattern. Instead of modifying
 * state directly, it calls the onMove lambda to report the desired change.
 * This prevents the composable from causing recompositions in the parent.
 *
 * @param onMove A function that is called with the requested change in x and z.
 */
@Composable
fun CylinderControls(
    onMove: (dx: Float, dz: Float) -> Unit
) {
    val moveAmount = 0.05f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // UP (-Z direction)
        Button(onClick = { onMove(0f, -moveAmount) }) {
            Text("Up")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // LEFT (-X direction)
            Button(onClick = { onMove(-moveAmount, 0f) }) {
                Text("Left")
            }
            // RIGHT (+X direction)
            Button(onClick = { onMove(moveAmount, 0f) }) {
                Text("Right")
            }
        }
        // DOWN (+Z direction)
        Button(onClick = { onMove(0f, moveAmount) }) {
            Text("Down")
        }
    }
}

/**
 * A full-screen overlay with a spinner and status text, used during model loading.
 */
@Composable
fun LoadingView(statusText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color.White
            )
            Text(
                text = statusText,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * A full-screen overlay that displays an error message.
 */
@Composable
fun ErrorView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
