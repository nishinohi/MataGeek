package com.example.matageek.manager

import com.example.matageek.fruity.module.MatageekModule

interface MeshAccessObserver {
    fun update(deviceInfo: DeviceInfo)
}

data class DeviceInfo(
    val clusterSize: Short? = null,
    val batteryInfo: Byte? = null,
    val trapState: Boolean? = null,
    val deviceName: String? = null,
    val matageekMode: MatageekModule.MatageekMode? = null
)