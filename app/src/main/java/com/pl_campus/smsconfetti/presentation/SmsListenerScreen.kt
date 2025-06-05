package com.pl_campus.smsconfetti.presentation

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Path
import com.pl_campus.smsconfetti.utils.ShapeState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pl_campus.smsconfetti.R
import com.pl_campus.smsconfetti.utils.FallingShape


@Composable
fun SmsListenerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasSmsPermission = isGranted
        }
    )


    var receiver by remember {
        mutableStateOf(false)
    }
    // Create the receiver instance, remembered across recompositions
    val smsReceiver = remember {
        SmsReceiverReceiver {
            receiver = true
        }

    }

    if(receiver){
        StaggeredFallAnimationScreen()
    }


    // Effect for registering and unregistering the receiver
    // Keys: context and hasSmsPermission. If either changes, the effect re-runs.
    DisposableEffect(context, hasSmsPermission) {
        if (hasSmsPermission) {
            val intentFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            // On Android versions that require it, you might need to specify receiver export status.
            // For dynamically registered receivers that are not exported, this is the default.
            // context.registerReceiver(smsReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED) // For API 33+ if needed explicitly
            context.registerReceiver(smsReceiver, intentFilter)
            // Cleanup when the composable leaves the composition or keys change
            onDispose {
                context.unregisterReceiver(smsReceiver)
            }
        } else {
            // If permission is not (or no longer) granted, do nothing for registration
            // The onDispose from a previous successful registration (if any) will have cleaned up.
            onDispose {
                // No action needed here if it wasn't registered in this run of the effect
            }
        }
    }

    // Handle lifecycle events to re-check permission if user changes it in settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permissionLauncher) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) { // Or ON_RESUME
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED && !hasSmsPermission
                ) {
                    // If permission was previously denied and still not granted, you might prompt again or update UI.
                    // Or if you want to request every time the screen becomes active and permission is missing:
                 //    permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED && !hasSmsPermission) {
                    // Permission granted in settings while app was in background
                    hasSmsPermission = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (!hasSmsPermission) {
        SideEffect {
            permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }
    }
}




@Composable
fun StaggeredFallAnimationScreen() {
    val numberOfShapes: Int = 150
    val shapes = remember { mutableStateListOf<FallingShape>() }
    var screenWidthPx by remember { mutableStateOf(0f) }
    var screenHeightPx by remember { mutableStateOf(0f) }
    var triggerFall by remember { mutableStateOf(0) } // Change to trigger re-fall

    val density = LocalDensity.current
    val shapeSize = 15.dp
    val shapeSizePx = with(density) { shapeSize.toPx() }

    // Initialize or re-initialize shapes when screen dimensions are known or trigger changes
    LaunchedEffect(screenWidthPx, screenHeightPx, triggerFall) {
        if (screenWidthPx == 0f || screenHeightPx == 0f) return@LaunchedEffect

        shapes.clear() // Clear previous shapes for re-fall
        for (i in 0 until numberOfShapes) {
            shapes.add(
                FallingShape(
                    id = i,
                    color = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)),
                    initialXOffsetPx = (Random.nextFloat() * (screenWidthPx - shapeSizePx)),
                    animatableY = Animatable(-shapeSizePx), // Start above the screen,
                    shapeState = ShapeState.entries.toTypedArray().random()
                )
            )
        }

        // Coroutine to manage the staggered fall
        launch {
            shapes.forEachIndexed { index, shape ->
                if (!shape.hasStartedFalling) { // Ensure it only starts once per trigger

                    launch { // Launch a new coroutine for each shape's animation
                        val theDelayBetweenShapesMs: Long = (0..40).random().toLong()
                        delay(index * theDelayBetweenShapesMs) // Stagger the start
                        shape.hasStartedFalling = true
                        shape.animatableY.animateTo(
                            targetValue = screenHeightPx, // Fall to the bottom edge (or slightly beyond)
                            animationSpec = tween(
                                durationMillis = Random.nextInt(1000, 1500), // Random fall duration
                                easing = LinearEasing
                            )
                        )
                        // Optionally, reset or remove the shape after it falls
                        // For this example, they just stay at the bottom
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                screenWidthPx = it.width.toFloat()
                screenHeightPx = it.height.toFloat()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.congratulations),
                contentDescription = "Full Screen Background Image", // Provide a meaningful description
                modifier = Modifier.fillMaxSize(), // Make the Image fill its parent Box
                contentScale = ContentScale.FillBounds // Or ContentScale.FillBounds, ContentScale.Fit
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                shapes.forEach { shape ->
                    when(shape.shapeState){
                        ShapeState.RECTANGLE -> {
                            val rectTopLeft = Offset(shape.initialXOffsetPx, shape.animatableY.value)
                            val rectSize = Size(shapeSizePx, shapeSizePx)
                            val pivotPoint = Offset(
                                x = rectTopLeft.x + rectSize.width / 2,
                                y = rectTopLeft.y + rectSize.height / 2
                            )
                            val degree = ((shape.animatableY.value/10) % 360).toFloat()
                            rotate(degrees = degree, pivot = pivotPoint){
                                drawRect(
                                    color = shape.color,
                                    topLeft = rectTopLeft,
                                    size = rectSize
                                    )
                            }

                        }
                        ShapeState.CIRCLE -> {
                            drawCircle(
                                color = shape.color,
                                center = Offset(shape.initialXOffsetPx, shape.animatableY.value + shapeSizePx),
                                radius = shapeSizePx/2
                            )
                        }
                        ShapeState.TRIANGLE -> {
                            val degree = ((shape.animatableY.value/10) % 360).toFloat()
                            val point1 = Offset(shape.initialXOffsetPx + shapeSizePx , shapeSizePx / 2 + shape.animatableY.value + shapeSizePx)             // Top point
                            val point2 = Offset(shape.initialXOffsetPx + shapeSizePx / 2, shapeSizePx * 6 / 4 + shape.animatableY.value + shapeSizePx)         // Bottom-left point
                            val point3 = Offset(shape.initialXOffsetPx + shapeSizePx * 6 / 4, shapeSizePx * 6 / 4 + shape.animatableY.value + shapeSizePx)     // Bottom-right point


                            val pivotPoint = Offset(
                                x = (point1.x + point2.x + point3.x)/3 ,
                                y = (point1.y + point2.y + point3.y)/3
                            )

                            // Create a Path object
                            val trianglePath = Path().apply {
                                moveTo(point1.x, point1.y) // Move to the first point
                                lineTo(point2.x, point2.y) // Draw a line to the second point
                                lineTo(point3.x, point3.y) // Draw a line to the third point
                                close() // Close the path to connect the last point to the first
                            }

                            rotate(degrees = degree, pivot = pivotPoint){
                                drawPath(
                                    path = trianglePath,
                                    color =shape.color
                                )
                            }

                        }
                    }
                }
            }

        }
    }
}