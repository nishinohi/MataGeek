package com.example.matageek.manager

import com.example.matageek.fruity.module.MatageekModule

interface DeviceInfoObserver {
    fun updateDeviceInfo(deviceInfo: DeviceInfo)
}

data class DeviceInfo(
    val nodeId: Short? = null,
    val clusterSize: Short? = null,
    val batteryInfo: Byte? = null,
    val trapState: Boolean? = null,
    val deviceName: String? = null,
    val matageekMode: MatageekModule.MatageekMode? = null
)