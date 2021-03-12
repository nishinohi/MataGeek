package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.matageek.R
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.manager.MeshAccessManager
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class DeviceConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val meshAccessManager: MeshAccessManager = MeshAccessManager(application)
    val connectionState: LiveData<ConnectionState> = meshAccessManager.state
    val deviceName: MutableLiveData<String> = MutableLiveData()
    val clusterSize = meshAccessManager.clusterSize
    val battery = meshAccessManager.batteryInfo
    private val deviceNamePreferences =
        application.getSharedPreferences(application.getString(R.string.preference_device_name_key),
            Context.MODE_PRIVATE)

    lateinit var discoveredDevice: DiscoveredDevice

    // TODO use String Provider
    fun connect(discoveredDevice: DiscoveredDevice) {
        this.discoveredDevice = discoveredDevice
        reconnect(discoveredDevice.device)
        val application = getApplication<Application>()
        this.deviceName.postValue(deviceNamePreferences.getString(discoveredDevice.device.address,
            application.getString(R.string.default_device_name)))
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

    fun setDeviceName(deviceName: String) {
        with(deviceNamePreferences.edit()) {
            putString(discoveredDevice.device.address, deviceName)
            commit()
        }
        this.deviceName.postValue(deviceName)
    }

    // TODO implement failed
    fun disconnect() {
        meshAccessManager.disconnect().enqueue()
    }

    fun loadDeviceName() {
        deviceName.postValue(deviceNamePreferences.getString(discoveredDevice.device.address, "unknown node"))
    }

    fun sendEnrollmentBroadcastAppStart() {
        meshAccessManager.sendEnrollmentBroadcastAppStart()
    }


}