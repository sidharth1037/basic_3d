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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.math.Position

/**
 * Displays the X, Y, Z coordinates of a position.
 */
@Composable
fun PositionLabel(position: Position) {
    Text(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp),
        color = Color.White,
        text = "X=%.2f, Y=%.2f, Z=%.2f".format(
            position.x,
            position.y,
            position.z
        )
    )
}

/**
 * Provides directional buttons to move a node.
 */
@Composable
fun CylinderControls(
    cylinderUiPosition: MutableState<Position>
) {
    val moveAmount = 0.05f

    // This function handles updating the position state.
    val onMove: (Position) -> Unit = { newPos ->
        cylinderUiPosition.value = newPos
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // FIX: "Up" should be negative Z, "Down" should be positive Z.
        Button(onClick = { onMove(cylinderUiPosition.value.copy(z = cylinderUiPosition.value.z - moveAmount)) }) {
            Text("Up")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onMove(cylinderUiPosition.value.copy(x = cylinderUiPosition.value.x - moveAmount)) }) {
                Text("Left")
            }
            Button(onClick = { onMove(cylinderUiPosition.value.copy(x = cylinderUiPosition.value.x + moveAmount)) }) {
                Text("Right")
            }
        }
        Button(onClick = { onMove(cylinderUiPosition.value.copy(z = cylinderUiPosition.value.z + moveAmount)) }) {
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
