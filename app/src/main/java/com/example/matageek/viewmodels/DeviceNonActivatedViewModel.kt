package com.example.matageek.viewmodels

import android.app.Application

class DeviceNonActivatedViewModel(application: Application) :
    AbstractDeviceConfigViewModel(application) {

    val clusterSize = meshAccessManager.clusterSize
    val battery = meshAccessManager.batteryInfo

    fun sendEnrollmentBroadcastAppStart() {
        meshAccessManager.sendEnrollmentBroadcastAppStart()
    }

}