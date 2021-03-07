package com.example.matageek.manager

import android.app.ApplicationErrorReport

interface MeshAccessObserver {
    abstract fun update(deviceInfo: DeviceInfo)
}

data class DeviceInfo(val clusterSize: Short?, val batteryInfo: Byte?)