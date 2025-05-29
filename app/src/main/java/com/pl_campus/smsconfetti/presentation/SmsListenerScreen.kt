package com.pl_campus.smsconfetti.presentation

import android.Manifest
import android.content.Context
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
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.pl_campus.smsconfetti.R

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
        GifAnimationFromFile(modifier, context)
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
fun GifAnimationFromFile(modifier: Modifier = Modifier, context: Context) {
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                // Add GIF decoder
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(R.drawable.cong_file)
            .build(),
        contentDescription = "My Animated GIF",
        imageLoader = imageLoader,
        modifier.fillMaxSize()
    )
}