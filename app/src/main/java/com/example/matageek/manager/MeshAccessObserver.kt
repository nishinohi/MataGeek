package com.example.matageek.manager

interface MeshAccessObserver {
    abstract fun update(deviceInfo: DeviceInfo)
}

data class DeviceInfo(
    val clusterSize: Short? = null,
    val batteryInfo: Byte? = null,
    val trapState: Boolean? = null,
    val deviceName: String? = null,
)