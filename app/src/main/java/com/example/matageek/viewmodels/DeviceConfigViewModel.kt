package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.matageek.manager.MataGeekBleManager
import no.nordicsemi.android.ble.callback.FailCallback
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class DeviceConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val mataGeekBleManager: MataGeekBleManager = MataGeekBleManager(application)
    val connectionState: LiveData<ConnectionState> = mataGeekBleManager.state
    val deviceName = mataGeekBleManager.deviceName
    val clusterSize = mataGeekBleManager.clusterSize
    val battery = mataGeekBleManager.battery

    lateinit var device: BluetoothDevice

    fun connect(device: BluetoothDevice) {
        this.device = device
        reconnect(device)
    }

    private fun reconnect(device: BluetoothDevice) {
        mataGeekBleManager.connect(device).retry(3, 100)
            .useAutoConnect(false)
            .enqueue()
    }

    fun startHandShake() {
        if (connectionState.value == ConnectionState.Ready) {
            mataGeekBleManager.startEncryptionHandshake()
        }
    }

    fun sendGetStatusMessage() {
        mataGeekBleManager.sendGetStatusMessage()
    }

    // TODO implement failed
    fun disconnect() {
        mataGeekBleManager.disconnect().enqueue()
    }


}