package com.example.matageek.adapter

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

class DiscoveredDevice(var lastScanResult: ScanResult) {
    val device: BluetoothDevice = lastScanResult.device
    var name: String? = null
    var rssi = 0
    var previousRssi = 0
    var highestRssi = -128

    fun update(scanResult: ScanResult) {
        lastScanResult = scanResult
        name = scanResult.scanRecord?.deviceName
        previousRssi = rssi
        rssi = scanResult.rssi
        highestRssi = if (highestRssi > rssi) highestRssi else rssi
    }

}