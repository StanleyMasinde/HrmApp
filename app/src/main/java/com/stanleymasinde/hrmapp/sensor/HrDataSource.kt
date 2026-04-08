package com.stanleymasinde.hrmapp.sensor

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType

class HrDataSource(
    private val context: Context,
    private val onHeartRateChanged: (Int) -> Unit,
    private val onError: (String) -> Unit,
    private val onAvailabilityChanged: (Boolean) -> Unit
) {
    private val measureClient = HealthServices.getClient(context).measureClient
    private var isRegistered = false

    private val heartRateCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            Log.d(TAG, "Availability changed: $availability")
            if (dataType == DataType.HEART_RATE_BPM) {
                val available = availability == DataTypeAvailability.AVAILABLE
                onAvailabilityChanged(available)
                if (!available) {
                    onError("Heart rate sensor unavailable")
                }
            }
        }

        override fun onDataReceived(data: DataPointContainer) {
            val heartRatePoints = data.getData(DataType.HEART_RATE_BPM)
            heartRatePoints.lastOrNull()?.let { point ->
                val bpm = point.value.toInt()
                Log.d(TAG, "Data received: $bpm BPM")
                onAvailabilityChanged(true)
                onHeartRateChanged(bpm)
            }
        }
    }

    fun start(): Boolean {
        if (isRegistered) return true
        try {
            measureClient.registerMeasureCallback(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
            isRegistered = true
            onAvailabilityChanged(false)
            Log.d(TAG, "Heart rate callback registered")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register heart rate callback", e)
            onError("Unable to read heart rate data")
            return false
        }
    }

    fun stop() {
        if (!isRegistered) return
        try {
            measureClient.unregisterMeasureCallbackAsync(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
            isRegistered = false
            onAvailabilityChanged(false)
            Log.d(TAG, "Heart rate callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister heart rate callback", e)
        }
    }

    companion object {
        private const val TAG = "HrDataSource"
    }
}
