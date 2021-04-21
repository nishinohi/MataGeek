package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.fruity.module.MatageekModule
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.DeviceInfoObserver
import com.example.matageek.manager.MeshAccessManager
import no.nordicsemi.android.ble.livedata.state.ConnectionState

abstract class AbstractDeviceConfigViewModel(application: Application) :
    AndroidViewModel(application), DeviceInfoObserver {
    protected val meshAccessManager: MeshAccessManager = MeshAccessManager(application)
    val currentNodeId: MutableLiveData<Short> = MutableLiveData()
    val connectionState: LiveData<ConnectionState> = meshAccessManager.state
    val handShakeState = meshAccessManager.handShakeState
    val deviceName: MutableLiveData<String> = MutableLiveData()
    val progressState: MutableLiveData<Boolean> = MutableLiveData()
    /** device config */
    val clusterSize: MutableLiveData<Short> = MutableLiveData()
    val batteryInfo: MutableLiveData<Byte> = MutableLiveData()
    val trapState: MutableLiveData<Boolean> = MutableLiveData()
    val modeState: MutableLiveData<MatageekModule.MatageekMode> = MutableLiveData()

    private lateinit var discoveredDevice: DiscoveredDevice

    fun addDeviceInfoObserver() {
        meshAccessManager.addDeviceInfoObserver(this)
    }

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

    override fun updateDeviceInfo(deviceInfo: DeviceInfo) {
        deviceInfo.nodeId?.let { currentNodeId.postValue(it) }
        deviceInfo.clusterSize?.let { clusterSize.postValue(it) }
        deviceInfo.batteryInfo?.let { batteryInfo.postValue(it) }
        deviceInfo.trapState?.let { trapState.postValue(it) }
        deviceInfo.matageekMode?.let { modeState.postValue(it) }
        deviceInfo.deviceName?.let { deviceName.postValue(it) }
    }

    fun updateCurrentNodeIdByPartnerId() {
        currentNodeId.postValue(meshAccessManager.getPartnerId())
    }

}