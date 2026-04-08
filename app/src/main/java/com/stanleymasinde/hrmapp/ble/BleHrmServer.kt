package com.stanleymasinde.hrmapp.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log

class BleHrmServer(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    private var connectedDevice: BluetoothDevice? = null
    private var notificationsEnabled = false

    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onAdvertisingStateChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Device connected: ${device.address}")
                connectedDevice = device
                onConnectionStateChanged?.invoke(true)
                // Stop advertising once connected to save power/avoid multiple connections
                stopAdvertising()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Device disconnected: ${device.address}")
                connectedDevice = null
                notificationsEnabled = false
                onConnectionStateChanged?.invoke(false)
                // Restart advertising so other devices (or the same one) can reconnect
                startAdvertising()
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            if (HrmUuids.CCCD == descriptor.uuid) {
                notificationsEnabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                Log.d(TAG, "Notifications enabled: $notificationsEnabled")

                if (responseNeeded) {
                    try {
                        bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException while sending response: ${e.message}")
                    }
                }
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "BLE Advertisement started successfully")
            onAdvertisingStateChanged?.invoke(true)
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE Advertisement failed with error code: $errorCode")
            onAdvertisingStateChanged?.invoke(false)
            onError?.invoke("Unable to start BLE advertising")
        }
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is disabled or not supported")
            onAdvertisingStateChanged?.invoke(false)
            onError?.invoke("Enable Bluetooth to broadcast heart rate")
            return false
        }

        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "BLE advertising is not available")
            onAdvertisingStateChanged?.invoke(false)
            onError?.invoke("BLE advertising is unavailable on this watch")
            return false
        }

        stop()
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        if (bluetoothGattServer == null) {
            Log.e(TAG, "Unable to open GATT server")
            onAdvertisingStateChanged?.invoke(false)
            onError?.invoke("Unable to open BLE server")
            return false
        }

        if (!setupGattService()) {
            stop()
            return false
        }
        return startAdvertising()
    }

    @SuppressLint("MissingPermission")
    private fun setupGattService(): Boolean {
        val hrmService = BluetoothGattService(HrmUuids.HEART_RATE_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        
        val hrmCharacteristic = BluetoothGattCharacteristic(
            HrmUuids.HR_MEASUREMENT,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccd = BluetoothGattDescriptor(
            HrmUuids.CCCD,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        
        hrmCharacteristic.addDescriptor(cccd)
        hrmService.addCharacteristic(hrmCharacteristic)
        val added = bluetoothGattServer?.addService(hrmService) == true
        if (!added) {
            Log.e(TAG, "Unable to add heart rate GATT service")
            onError?.invoke("Unable to publish BLE heart rate service")
        }
        return added
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising(): Boolean {
        val advertiser = bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "BLE advertiser unavailable")
            onAdvertisingStateChanged?.invoke(false)
            onError?.invoke("BLE advertising is unavailable on this watch")
            return false
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(HrmUuids.HEART_RATE_SERVICE))
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        onAdvertisingStateChanged?.invoke(false)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        stopAdvertising()
        bluetoothGattServer?.close()
        bluetoothGattServer = null
        connectedDevice = null
        notificationsEnabled = false
    }

    @SuppressLint("MissingPermission")
    fun updateHeartRate(bpm: Int) {
        val device = connectedDevice ?: return
        if (!notificationsEnabled) return

        val characteristic = bluetoothGattServer
            ?.getService(HrmUuids.HEART_RATE_SERVICE)
            ?.getCharacteristic(HrmUuids.HR_MEASUREMENT) ?: return

        // 0x2A37 packet: flags byte + BPM byte
        // flags = 0x00 means BPM is UINT8 (valid up to 255 bpm)
        val payload = byteArrayOf(0x00, bpm.toByte())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false, payload)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = payload
            @Suppress("DEPRECATION")
            bluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    companion object {
        private const val TAG = "BleHrmServer"
    }
}
