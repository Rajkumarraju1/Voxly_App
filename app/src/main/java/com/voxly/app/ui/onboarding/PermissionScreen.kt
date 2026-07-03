package com.voxly.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.voxly.app.ui.components.*

@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    // Dynamic state trackers for each permission
    var isAudioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isNotificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    if (com.voxly.app.BuildConfig.DEBUG) {
        Log.d("PERMISSION_TRACE", "PermissionScreen recomposed. audio=$isAudioGranted, camera=$isCameraGranted, notifications=$isNotificationsGranted")
    }

    // Re-check permissions when screen is resumed (e.g., user returns from system settings or system dialog)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val audioCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                val cameraCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                val notificationsCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                
                if (com.voxly.app.BuildConfig.DEBUG) {
                    Log.d("PERMISSION_TRACE", "ON_RESUME sync - audio=$audioCheck, camera=$cameraCheck, notifications=$notificationsCheck")
                }
                isAudioGranted = audioCheck
                isCameraGranted = cameraCheck
                isNotificationsGranted = notificationsCheck
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var permissionRequestCount by remember { mutableIntStateOf(0) }
    var isCheckingRequiredPermissions by remember { mutableStateOf(false) }

    fun checkAndProceed() {
        if (com.voxly.app.BuildConfig.DEBUG) {
            Log.d("PERMISSION_TRACE", "checkAndProceed() called: audio=$isAudioGranted, camera=$isCameraGranted")
        }
        if (isAudioGranted && isCameraGranted) {
            if (com.voxly.app.BuildConfig.DEBUG) {
                Log.d("PERMISSION_TRACE", "Required permissions met. Proceeding to onContinue().")
            }
            onContinue()
        } else {
            if (com.voxly.app.BuildConfig.DEBUG) {
                Log.d("PERMISSION_TRACE", "Required permissions NOT met. Showing rationale dialog.")
            }
            showRationale = true
        }
    }

    LaunchedEffect(permissionRequestCount) {
        if (permissionRequestCount > 0 && isCheckingRequiredPermissions) {
            if (com.voxly.app.BuildConfig.DEBUG) {
                Log.d("PERMISSION_TRACE", "LaunchedEffect triggered check. count=$permissionRequestCount")
            }
            isCheckingRequiredPermissions = false
            checkAndProceed()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            if (com.voxly.app.BuildConfig.DEBUG) {
                Log.d("PERMISSION_TRACE", "Launcher callback onResult received map: $result")
            }
            isAudioGranted = result[Manifest.permission.RECORD_AUDIO] ?: isAudioGranted
            isCameraGranted = result[Manifest.permission.CAMERA] ?: isCameraGranted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isNotificationsGranted = result[Manifest.permission.POST_NOTIFICATIONS] ?: isNotificationsGranted
            }
            if (com.voxly.app.BuildConfig.DEBUG) {
                Log.d("PERMISSION_TRACE", "Launcher callback updated states: audio=$isAudioGranted, camera=$isCameraGranted, notifications=$isNotificationsGranted")
            }
            permissionRequestCount++
        }
    )

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(text = "Permissions Required", color = Color.White) },
            text = { 
                Text(
                    text = "Voxly requires Microphone and Camera access to function correctly for calls. Please grant these permissions in Settings.",
                    color = Color.Gray
                ) 
            },
            containerColor = Color(0xFF141419),
            confirmButton = {
                TextButton(
                    onClick = { 
                        showRationale = false
                        isCheckingRequiredPermissions = true
                        launcher.launch(permissionsToRequest)
                    }
                ) {
                    Text("Try Again", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    OnboardingScaffold(
        onBackClick = onBackClick,
        progressIndicatorSlot = {
            OnboardingProgressIndicator(
                currentStep = 5,
                totalSteps = 5
            )
        },
        headerSlot = {
            Column {
                Text(
                    text = buildAnnotatedString {
                        append("Enable ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Access")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To provide the best calling experience, Voxly needs access to a few things.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
            }
        },
        bottomActionsSlot = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Gold Style VoxlyPrimaryButton variant
                VoxlyPrimaryButton(
                    text = "Allow Access",
                    onClick = {
                        if (com.voxly.app.BuildConfig.DEBUG) {
                            Log.d("PERMISSION_TRACE", "Allow Access button clicked. audio=$isAudioGranted, camera=$isCameraGranted")
                        }
                        if (isAudioGranted && isCameraGranted) {
                            if (com.voxly.app.BuildConfig.DEBUG) {
                                Log.d("PERMISSION_TRACE", "Allow Access clicked: Required permissions already met. Running checkAndProceed().")
                            }
                            checkAndProceed()
                        } else {
                            if (com.voxly.app.BuildConfig.DEBUG) {
                                Log.d("PERMISSION_TRACE", "Allow Access clicked: Required permissions missing. Setting isCheckingRequiredPermissions=true and launching full prompt.")
                            }
                            isCheckingRequiredPermissions = true
                            launcher.launch(permissionsToRequest)
                        }
                    },
                    variant = VoxlyButtonVariant.GOLD,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = {
                        android.widget.Toast.makeText(context, "Calls won't work without permissions!", android.widget.Toast.LENGTH_SHORT).show()
                        onContinue()
                    }
                ) {
                    Text(
                        text = "Skip for now",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Reusable Permission Cards
            PermissionCard(
                title = "Microphone",
                description = "To talk with other users",
                icon = Icons.Default.Mic,
                isGranted = isAudioGranted,
                onClick = { 
                    if (com.voxly.app.BuildConfig.DEBUG) {
                        Log.d("PERMISSION_TRACE", "Microphone card clicked. isAudioGranted=$isAudioGranted. Launching RECORD_AUDIO permission request.")
                    }
                    launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) 
                }
            )

            PermissionCard(
                title = "Camera",
                description = "For video calls",
                icon = Icons.Default.Videocam,
                isGranted = isCameraGranted,
                onClick = { 
                    if (com.voxly.app.BuildConfig.DEBUG) {
                        Log.d("PERMISSION_TRACE", "Camera card clicked. isCameraGranted=$isCameraGranted. Launching CAMERA permission request.")
                    }
                    launcher.launch(arrayOf(Manifest.permission.CAMERA)) 
                }
            )

            PermissionCard(
                title = "Notifications",
                description = "To receive incoming calls",
                icon = Icons.Default.Notifications,
                isGranted = isNotificationsGranted,
                onClick = { 
                    if (com.voxly.app.BuildConfig.DEBUG) {
                        Log.d("PERMISSION_TRACE", "Notifications card clicked. isNotificationsGranted=$isNotificationsGranted. Launching POST_NOTIFICATIONS permission request.")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoxlyInfoBanner(
                description = "Your privacy is important to us. We never share your data.",
                icon = Icons.Default.Security,
                style = BannerStyle.INFO
            )
        }
    }
}
