package com.example.mapview

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.filament.EntityManager
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

@Composable
fun MainScene() {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)

    val lightNode = remember(engine) {
        val lightEntity = EntityManager.get().create()
        com.google.android.filament.LightManager.Builder(com.google.android.filament.LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(100_000.0f)
            .direction(0.0f, -1.0f, 0.0f)
            .castShadows(true)
            .build(engine, lightEntity)
        LightNode(engine = engine, entity = lightEntity).apply {
            rotation = Rotation(x = -45.0f, y = 0.0f, z = 0.0f)
        }
    }

    // This state variable is the single source of truth for the cylinder's world position.
    val cylinderUiPosition = remember { mutableStateOf(Position(x = 0.85f, y = 0.015f, z = 0.25f)) }

    val redMaterial = remember(engine) { materialLoader.createColorInstance(color = androidx.compose.ui.graphics.Color.Red) }

    val cylinderNode = remember(redMaterial) {
        CylinderNode(
            engine = engine,
            radius = 0.01f,
            height = 0.03f,
            // FIX: Don't use the world position for the center. The center should be relative to the node's own origin.
            // Using Position() centers the geometry on the node's anchor point.
            center = Position(),
            materialInstance = redMaterial
        )
    }

    // This effect listens for changes in the UI state and updates the actual 3D node's position.
    LaunchedEffect(cylinderUiPosition.value) {
        cylinderNode.position = cylinderUiPosition.value
    }

    val childNodes = rememberNodes {
        add(lightNode)
        add(cylinderNode)
    }

    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("Initializing...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Set the initial position of the node once, when the effect first runs.
        // This avoids compounding the position on hot reloads.
        cylinderNode.position = cylinderUiPosition.value

        delay(100)
        try {
            val buffer = withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) { statusText = "Reading File..." }
                context.assets.open("aromal.glb").use { input ->
                    ByteBuffer.wrap(input.readBytes())
                }
            }
            withContext(Dispatchers.Main) {
                statusText = "Parsing 3D Data..."
                val instance = modelLoader.createModelInstance(buffer)
                statusText = "Creating Node..."
                val node = ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 2.0f,
                    centerOrigin = Position(x = 0.0f, y = 0.0f, z = 0.0f)
                ).apply {
                    rotation = Rotation(x = 0.0f, y = 0.0f, z = 0.0f)
                }
                childNodes.add(node)
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e("SceneView", "Load Error", e)
            withContext(Dispatchers.Main) {
                errorMessage = "Crash: ${e.message}"
                isLoading = false
            }
        }
    }

    val cameraManipulator = rememberCameraManipulator()

    Box(modifier = Modifier.fillMaxSize()) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            childNodes = childNodes,
            cameraManipulator = cameraManipulator
        )

        // --- UI OVERLAYS ---
        Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            PositionLabel(cylinderUiPosition.value)
        }

        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            CylinderControls(cylinderUiPosition)
        }

        if (isLoading) {
            LoadingView(statusText)
        }

        errorMessage?.let {
            ErrorView(it)
        }
    }
}
