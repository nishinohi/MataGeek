package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.matageek.R
import com.example.matageek.manager.MataGeekBleManager
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class DeviceConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val mataGeekBleManager: MataGeekBleManager = MataGeekBleManager(application)
    val connectionState: LiveData<ConnectionState> = mataGeekBleManager.state
    val deviceName: MutableLiveData<String> = MutableLiveData()
    val clusterSize = mataGeekBleManager.clusterSize
    val battery = mataGeekBleManager.battery
    private val deviceNamePreferences =
        application.getSharedPreferences(application.getString(R.string.preference_device_name_key),
            Context.MODE_PRIVATE)

    lateinit var device: BluetoothDevice

    // TODO use String Provider
    fun connect(device: BluetoothDevice) {
        this.device = device
        reconnect(device)
        val application = getApplication<Application>()
        this.deviceName.postValue(deviceNamePreferences.getString(device.address,
            application.getString(R.string.default_device_name)))
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

    fun setDeviceName(deviceName: String) {
        with(deviceNamePreferences.edit()) {
            putString(device.address, deviceName)
            commit()
        }
        this.deviceName.postValue(deviceName)
    }

    fun sendGetStatusMessage() {
        mataGeekBleManager.sendGetStatusMessage()
    }

    // TODO implement failed
    fun disconnect() {
        mataGeekBleManager.disconnect().enqueue()
    }

    fun loadDeviceName() {
        deviceName.postValue(deviceNamePreferences.getString(device.address, "unknown node"))
    }


}