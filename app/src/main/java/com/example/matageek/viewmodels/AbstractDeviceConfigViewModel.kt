package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.MeshAccessManager
import no.nordicsemi.android.ble.livedata.state.ConnectionState

abstract class AbstractDeviceConfigViewModel(application: Application) :
    AndroidViewModel(application) {
    protected val meshAccessManager: MeshAccessManager = MeshAccessManager(application)
    val connectionState: LiveData<ConnectionState> = meshAccessManager.state
    val handShakeState = meshAccessManager.handShakeState
    val deviceName: MutableLiveData<String> = MutableLiveData()
    val progressState: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var discoveredDevice: DiscoveredDevice

    fun connect(discoveredDevice: DiscoveredDevice) {
        this.discoveredDevice = discoveredDevice
        reconnect(discoveredDevice.device)
    }

    // TODO replace magic number
    private fun reconnect(device: BluetoothDevice) {
        meshAccessManager.connect(device).retry(3, 100)
            .useAutoConnect(false)
            .enqueue()
    }

    fun startHandShake() {
        if (connectionState.value == ConnectionState.Ready) {
            meshAccessManager.startEncryptionHandshake(discoveredDevice.enrolled)
        }
    }

    // TODO implement failed
    fun disconnect() {
        meshAccessManager.disconnect().enqueue()
    }

    fun inProgress() {
        progressState.postValue(true)
    }

    fun endProgress() {
        progressState.postValue(false)
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }

    fun update(deviceInfo: DeviceInfo) {
        deviceInfo.clusterSize?.let { meshAccessManager.clusterSize.postValue(it) }
        deviceInfo.batteryInfo?.let { meshAccessManager.batteryInfo.postValue(it) }
        deviceInfo.trapState?.let { meshAccessManager.trapState.postValue(it) }
        deviceInfo.deviceName?.let { deviceName.postValue(it) }
        deviceInfo.matageekMode?.let { meshAccessManager.modeState.postValue(it) }
    }

}