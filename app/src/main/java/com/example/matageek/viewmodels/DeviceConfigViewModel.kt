package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.matageek.manager.MeshAccessManager
import com.example.matageek.profile.callback.EncryptionState
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class DeviceConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val meshAccessManager: MeshAccessManager = MeshAccessManager(application)
    val connectionState: LiveData<ConnectionState> = meshAccessManager.state
    val encryptionState: LiveData<EncryptionState> = meshAccessManager.encryptionState
    lateinit var device: BluetoothDevice

    fun connect(device: BluetoothDevice) {
        this.device = device
        reconnect(device)
    }

    private fun reconnect(device: BluetoothDevice) {
        meshAccessManager.connect(device).retry(3, 100)
            .useAutoConnect(false)
            .enqueue()
    }

    fun startHandShake() {
        if (connectionState.value == ConnectionState.Ready) {
            meshAccessManager.startEncryptionHandshake()
        }
    }

}