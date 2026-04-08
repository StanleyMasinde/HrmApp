package com.stanleymasinde.hrmapp.presentation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.stanleymasinde.hrmapp.presentation.theme.HrmAppTheme
import com.stanleymasinde.hrmapp.service.HrmForegroundService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HrmAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hrmService by remember { mutableStateOf<HrmForegroundService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as HrmForegroundService.LocalBinder
                hrmService = binder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                hrmService = null
                isBound = false
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, HrmForegroundService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            if (isBound) {
                context.unbindService(connection)
                isBound = false
            }
        }
    }

    val heartRateState = hrmService?.heartRate?.collectAsState(initial = 0)
    val heartRate = heartRateState?.value ?: 0

    val isRunningState = hrmService?.isRunning?.collectAsState(initial = false)
    val isRunning = isRunningState?.value ?: false

    val isSensorAvailableState = hrmService?.isSensorAvailable?.collectAsState(initial = false)
    val isSensorAvailable = isSensorAvailableState?.value ?: false

    val statusMessageState = hrmService?.statusMessage?.collectAsState(initial = "Ready to broadcast")
    val statusMessage = statusMessageState?.value ?: "Ready to broadcast"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val standardForeground = remember {
            val list = mutableListOf(Manifest.permission.BODY_SENSORS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                list.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            list.toTypedArray()
        }

        val backgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "android.permission.BODY_SENSORS_BACKGROUND"
        } else {
            ""
        }

        var permissionStatuses by remember {
            mutableStateOf(getPermissionMap(context, standardForeground, backgroundPermission))
        }

        val foregroundGranted = standardForeground.all { permissionStatuses[it] == true }
        val backgroundGranted = if (backgroundPermission.isNotEmpty()) {
            permissionStatuses[backgroundPermission] == true
        } else {
            true
        }
        
        val canStart = foregroundGranted && backgroundGranted

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            permissionStatuses = permissionStatuses + result
        }

        val backgroundLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionStatuses = permissionStatuses + (backgroundPermission to isGranted)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    permissionStatuses = getPermissionMap(context, standardForeground, backgroundPermission)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (canStart) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Heart Rate", 
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = if (heartRate > 0) "$heartRate BPM" else "-- BPM",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRunning) Color(0xFF00E676) else Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isRunning) {
                    Text(
                        text = when {
                            heartRate > 0 -> "Sensor active"
                            isSensorAvailable -> "Waiting for heart rate..."
                            else -> "Check watch fit and sensor access"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (heartRate > 0 || isSensorAvailable) {
                            Color(0xFF00E676)
                        } else {
                            Color.LightGray
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { HrmForegroundService.stop(context) }) {
                        Text("Stop")
                    }
                } else {
                    Button(onClick = { HrmForegroundService.start(context) }) {
                        Text("Broadcast HR")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Permissions Needed", 
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                PermissionItem("Standard Sensors", foregroundGranted)
                PermissionItem("Background Access", backgroundGranted)

                Spacer(modifier = Modifier.height(16.dp))

                if (!foregroundGranted) {
                    Button(onClick = { 
                        permissionLauncher.launch(standardForeground) 
                    }) {
                        Text("Grant Basic")
                    }
                } else {
                    Text(
                        "Enable background sensor access in Settings before starting.",
                        style = MaterialTheme.typography.bodyExtraSmall,
                        textAlign = TextAlign.Center,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (backgroundPermission.isNotEmpty()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                openAppSettings(context)
                            } else {
                                backgroundLauncher.launch(backgroundPermission)
                            }
                        }
                    }) {
                        Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            "Open Settings"
                        } else {
                            "Allow All The Time"
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItem(label: String, isGranted: Boolean) {
    Text(
        text = "${if (isGranted) "✅" else "❌"} $label",
        style = MaterialTheme.typography.bodySmall,
        color = if (isGranted) Color(0xFF00E676) else Color.White
    )
}

private fun getPermissionMap(
    context: Context,
    foreground: Array<String>,
    background: String
): Map<String, Boolean> {
    val map = mutableMapOf<String, Boolean>()
    foreground.forEach { 
        val granted = ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        map[it] = granted
        Log.d("PermissionCheck", "Permission: $it, Granted: $granted")
    }
    if (background.isNotEmpty()) {
        val granted = ContextCompat.checkSelfPermission(context, background) == PackageManager.PERMISSION_GRANTED
        map[background] = granted
        Log.d("PermissionCheck", "Permission: $background, Granted: $granted")
    }
    return map
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
