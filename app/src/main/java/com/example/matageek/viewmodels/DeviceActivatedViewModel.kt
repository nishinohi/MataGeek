package com.example.matageek.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData

class DeviceActivatedViewModel(application: Application) :
    AbstractDeviceConfigViewModel(application) {

    val deviceName: MutableLiveData<String> = MutableLiveData()
    val clusterSize: MutableLiveData<Short> = meshAccessManager.clusterSize
    val battery: MutableLiveData<Byte> = meshAccessManager.batteryInfo
    val trapState: MutableLiveData<Boolean> = meshAccessManager.trapState


}