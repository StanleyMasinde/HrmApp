package com.stanleymasinde.hrmapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.stanleymasinde.hrmapp.R
import com.stanleymasinde.hrmapp.ble.BleHrmServer
import com.stanleymasinde.hrmapp.sensor.HrDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HrmForegroundService : Service() {

    private val binder = LocalBinder()
    private lateinit var bleHrmServer: BleHrmServer
    private lateinit var hrDataSource: HrDataSource

    private val _heartRate = MutableStateFlow(0)
    val heartRate = _heartRate.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(false)
    val isSensorAvailable = _isSensorAvailable.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to broadcast")
    val statusMessage = _statusMessage.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): HrmForegroundService = this@HrmForegroundService
    }

    companion object {
        const val ACTION_START = "com.stanleymasinde.hrmapp.START"
        const val ACTION_STOP  = "com.stanleymasinde.hrmapp.STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "hrm_service_channel"
        private const val TAG = "HrmForegroundService"

        fun start(context: Context) {
            val intent = Intent(context, HrmForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HrmForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        bleHrmServer = BleHrmServer(this).apply {
            onConnectionStateChanged = { connected ->
                _isConnected.value = connected
                refreshStatusMessage()
            }
            onAdvertisingStateChanged = { advertising ->
                _isAdvertising.value = advertising
                refreshStatusMessage()
            }
            onError = { message ->
                Log.e(TAG, message)
                _statusMessage.value = message
                _isRunning.value = false
                _isAdvertising.value = false
                handleStartupFailure()
            }
        }
        hrDataSource = HrDataSource(
            context = this,
            onHeartRateChanged = { bpm ->
                Log.d(TAG, "Callback received BPM: $bpm")
                _heartRate.value = bpm
                bleHrmServer.updateHeartRate(bpm)
                refreshStatusMessage()
                updateNotification(bpm)
            },
            onError = { message ->
                Log.e(TAG, message)
                if (_heartRate.value == 0) {
                    _statusMessage.value = message
                }
            },
            onAvailabilityChanged = { available ->
                _isSensorAvailable.value = available
                refreshStatusMessage()
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Starting HRM Service")
                startForegroundServiceInternal()
                _heartRate.value = 0
                _isConnected.value = false
                _isAdvertising.value = false
                _isSensorAvailable.value = false
                refreshStatusMessage()
                val bleStarted = bleHrmServer.start()
                val hrStarted = hrDataSource.start()
                if (bleStarted && hrStarted) {
                    _isRunning.value = true
                    refreshStatusMessage()
                } else {
                    handleStartupFailure()
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping HRM Service")
                stopWork()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceInternal() {
        ensureChannel()
        val notification = buildNotification(0)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(bpm: Int): Notification {
        val stopIntent = Intent(this, HrmForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (bpm > 0) "Current HR: $bpm BPM" else "Waiting for connection..."
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HRM Broadcasting")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStopIntent)
            .build()
    }

    private fun updateNotification(bpm: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(bpm))
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "HRM Background Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun stopWork() {
        Log.d(TAG, "Stopping work")
        _isRunning.value = false
        _isAdvertising.value = false
        _isConnected.value = false
        _isSensorAvailable.value = false
        _heartRate.value = 0
        refreshStatusMessage()
        bleHrmServer.stop()
        hrDataSource.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleStartupFailure() {
        _isRunning.value = false
        _isAdvertising.value = false
        _isConnected.value = false
        _isSensorAvailable.value = false
        _heartRate.value = 0
        bleHrmServer.stop()
        hrDataSource.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun refreshStatusMessage() {
        _statusMessage.value = when {
            !_isRunning.value -> "Ready to broadcast"
            _heartRate.value > 0 || _isSensorAvailable.value -> {
                if (_isConnected.value) "Connected to receiver" else "Advertising BLE..."
            }
            _isAdvertising.value -> "Waiting for heart rate..."
            else -> "Preparing BLE..."
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopWork()
        super.onDestroy()
    }
}
