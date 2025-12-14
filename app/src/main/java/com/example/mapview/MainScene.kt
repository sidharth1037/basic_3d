package com.example.mapview

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    val redMaterial = remember(engine) { materialLoader.createColorInstance(color = androidx.compose.ui.graphics.Color.Red) }

    val cylinderNode = remember(redMaterial) {
        CylinderNode(
            engine = engine,
            radius = 0.01f,
            height = 0.03f,
            center = Position(),
            materialInstance = redMaterial
        )
    }

    val childNodes = rememberNodes {
        add(lightNode)
        add(cylinderNode)
    }

    var isLoading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("Initializing...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Set the initial position of the cylinder node directly.
        cylinderNode.position = Position(x = 0.85f, y = 0.015f, z = 0.25f)

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
        Column(modifier = Modifier.fillMaxSize()) {
            // Top 80% for the 3D Scene
            Box(modifier = Modifier
                .weight(0.8f)
                .fillMaxWidth()) {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    childNodes = childNodes,
                    cameraManipulator = cameraManipulator
                )
            }
            // Bottom 20% for the controls
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Restore the movement logic since the test is complete.
                val onMove = remember {
                    { dx: Float, dz: Float ->
                        cylinderNode.position = cylinderNode.position.copy(
                            x = cylinderNode.position.x + dx,
                            z = cylinderNode.position.z + dz
                        )
                    }
                }
                CylinderControls(onMove = onMove)
            }
        }

        // Loading and error overlays are placed in the root Box to cover the whole screen
        if (isLoading) {
            LoadingView(statusText)
        }

        errorMessage?.let {
            ErrorView(it)
        }
    }
}
