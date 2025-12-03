package com.example.mapview

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
// FIX: Explicit imports to resolve lambda type errors
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Standard Jetpack Compose setup to make the app a full-screen Compose experience.
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // A Box is used to potentially layer UI elements over the 3D scene.
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // The composable that contains all the 3D rendering logic.
                        HeavyModelViewer()
                    }
                }
            }
        }
    }
}

@Composable
fun HeavyModelViewer() {
    // Get the current Android context, needed for accessing assets.
    val context = LocalContext.current
    // Remember the SceneView engine instance. This is the core of the rendering system.
    val engine = rememberEngine()
    // Remember the ModelLoader, a utility for creating 3D models from various sources.
    val modelLoader = rememberModelLoader(engine)

    // --- MANUAL LIGHT SETUP ---
    // In recent SceneView versions, creating a LightNode directly can cause errors.
    // This manual setup creates the underlying Filament light entity first, ensuring stability.
    val lightNode = remember(engine) {
        // Get the EntityManager, which is Filament's way of tracking all objects in the scene.
        val lightEntity = EntityManager.get().create()

        // Use the Filament LightManager to build a directional light.
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f) // White light
            .intensity(100_000.0f) // Brightness of the light
            .direction(0.0f, -1.0f, 0.0f) // Shines from top to bottom
            .castShadows(true) // Enables shadows
            .build(engine, lightEntity) // Associates the light with our engine and entity

        // Create the SceneView LightNode wrapper around the manually created light entity.
        LightNode(engine = engine, entity = lightEntity).apply {
            // You can further configure the node's position/rotation if needed.
            rotation = Rotation(x = -45.0f, y = 0.0f, z = 0.0f)
        }
    }

    // This holds the list of all nodes (objects) in our scene.
    // We initialize it with the light we just created.
    val childNodes = rememberNodes {
        add(lightNode)
    }

    // --- UI & LOADING STATE MANAGEMENT ---
    // These state variables control what the user sees (e.g., loading spinner, error message).
    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("Initializing...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // LaunchedEffect is used for side-effects, like loading data, that shouldn't block the UI.
    // `Unit` means this effect runs once when the composable is first displayed.
    LaunchedEffect(Unit) {
        // A small delay to ensure the UI has time to draw the initial loading state.
        delay(100)

        try {
            // --- TWO-STEP MODEL LOADING PROCESS ---

            // STEP 1: Read the binary GLB file from assets into a memory buffer.
            // This is done on the IO thread (`Dispatchers.IO`) to prevent freezing the UI.
            val buffer = withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) { statusText = "Reading File..." }
                context.assets.open("aromal.glb").use { input ->
                    val bytes = input.readBytes()
                    ByteBuffer.wrap(bytes) // Wrap the raw bytes in a buffer Filament can read.
                }
            }

            // STEP 2: Create the ModelInstance from the buffer.
            // This MUST be done on the main thread (`Dispatchers.Main`) because Filament's
            // rendering engine is not thread-safe. Accessing it from a background thread will crash the app.
            withContext(Dispatchers.Main) {
                statusText = "Parsing 3D Data..."

                // The ModelLoader takes the data buffer and prepares it for rendering.
                val instance = modelLoader.createModelInstance(buffer)

                statusText = "Creating Node..."

                // Create the final SceneView node for the model.
                val buildingNode = ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 2.0f, // Scale the model to be 2 meters in size.
                    centerOrigin = Position(x = 0.0f, y = 0.0f, z = 0.0f) // Center the model at its origin.
                ).apply {
                    isEditable = true // Allows the user to interact with the model (rotate, scale, etc.)
                    rotation = Rotation(x = 0.0f, y = 45.0f, z = 0.0f) // Set an initial rotation.
                }

                // Add the fully loaded model node to the scene and hide the loading indicator.
                childNodes.add(buildingNode)
                isLoading = false
            }
        } catch (e: Exception) {
            // If anything goes wrong during loading, log the error and show it to the user.
            Log.e("SceneView", "Load Error", e)
            withContext(Dispatchers.Main) {
                errorMessage = "Crash: ${e.message}"
                isLoading = false
            }
        }
    }

    // --- UI DRAWING ---
    Box(modifier = Modifier.fillMaxSize()) {
        // The main Scene composable that handles all the rendering.
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = childNodes // Provide the list of nodes to render.
        )

        // If `isLoading` is true, draw a semi-transparent overlay with a spinner.
        if (isLoading) {
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

        // If an error occurs, show a similar overlay with the error message.
        errorMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = msg,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
