package com.example.flashmobile

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.flashmobile.ui.theme.FlashmobileTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlashmobileTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(CameraManager::class.java) }
    val cameraId = remember { cameraManager?.cameraIdList?.firstOrNull() }

    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // use the proper ActivityResultContracts.RequestPermission and specify the result type
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var toggleOn by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(0f) } // 0..100

    // compute blink frequency: linear mapping from value 0->1Hz? user requested 0->flash on, 50->4Hz, 100->16Hz
    // We'll map 0 -> steady (continuous on) when toggleOn; for other values map 0..100 to 1..16 frequency, with 50->4
    // Find mapping f(v) = a * v + b, with f(50)=4, f(100)=16 => a*50 + b =4, a*100 + b =16 => subtract: 50a = 12 => a=0.24, b = 4 - 50*0.24 = 4 - 12 = -8.
    // f(0) = -8 -> negative; we will treat v==0 as special: continuous on. For v>0, f(v)=0.24*v -8, clamp min 1.

    val scope = rememberCoroutineScope()
    var blinkJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(toggleOn, value, hasCameraPermission, cameraId) {
        blinkJob?.cancel()
        if (toggleOn && hasCameraPermission && cameraId != null) {
            if (value == 0f) {
                // turn torch on continuously
                try {
                    cameraManager?.setTorchMode(cameraId, true)
                } catch (_: CameraAccessException) {
                    // ignore
                }
            } else {
                val freq = (0.24f * value - 8f).coerceAtLeast(1f) // Hz
                val periodMs = (1000f / freq).toLong()
                // we blink with that period: toggle torch on/off each period/2
                blinkJob = scope.launch {
                    try {
                        while (isActive) {
                            cameraManager?.setTorchMode(cameraId, true)
                            delay(periodMs / 2)
                            cameraManager?.setTorchMode(cameraId, false)
                            delay(periodMs / 2)
                        }
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            }
        } else {
            // ensure torch off
            try {
                if (cameraId != null) cameraManager?.setTorchMode(cameraId, false)
            } catch (_: Exception) {
                // ignore
            }
        }
        onDispose {
            blinkJob?.cancel()
            try {
                if (cameraId != null) cameraManager?.setTorchMode(cameraId, false)
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        // Semicircular gauge
        Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            SemiCircularGauge(value = value, onValueChange = { value = it })

            // Big toggle button
            val bgColor by animateColorAsState(if (toggleOn) Color.Yellow else Color.Gray)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { toggleOn = !toggleOn })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (toggleOn) "ON" else "OFF", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Value: ${value.roundToInt()}", color = Color.White)
    }
}

@Composable
fun SemiCircularGauge(value: Float, onValueChange: (Float) -> Unit) {
    // Draw a semicircular arc from left (180deg) to right (0deg) and allow dragging to change value
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    // size here is IntSize from PointerInputScope; convert to Float for Offset
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val touch = change.position
                    val dx = touch.x - center.x
                    val dy = touch.y - center.y
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    // angle is -180..180, 0 is to the right, 90 down, -90 up
                    // we want semicircle top half: from 180 (left) -> 0 (right) along the top, so angles from 180..0 via positive/negative? we'll map using atan2
                    // Convert angle to 0..360
                    if (angle < 0) angle += 360f
                    // we only accept angles from 180..360 (left-top to right-top)
                    if (angle in 180f..360f) {
                        // map 180..360 -> 0..100
                        val t = (angle - 180f) / 180f
                        onValueChange((t * 100f).coerceIn(0f, 100f))
                    }
                }
            }
        ) {
            val stroke = 20f
            val radius = size.minDimension / 2 - stroke
            val center = Offset(size.width / 2, size.height / 2)
            // draw background arc
            drawArc(
                color = Color.DarkGray,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            // draw progress arc
            val sweep = (value / 100f) * 180f
            drawArc(
                color = Color.Yellow,
                startAngle = 180f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FlashmobileTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            MainScreen()
        }
    }
}