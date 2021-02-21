package com.example.matageek.adapter

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

class DiscoveredDevice(var lastScanResult: ScanResult) {
    val device: BluetoothDevice = lastScanResult.device
    var name: String? = null
    var rssi = 0
    var previousRssi = 0
    var highestRssi = -128

    init {
        update(lastScanResult)
    }

    fun update(scanResult: ScanResult) {
        lastScanResult = scanResult
        name = scanResult.scanRecord?.deviceName
        previousRssi = rssi
        rssi = scanResult.rssi
        highestRssi = if (highestRssi > rssi) highestRssi else rssi
    }

    /* package */
    fun hasRssiLevelChanged(): Boolean {
        val newLevel =
            if (rssi <= 10) 0 else if (rssi <= 28) 1 else if (rssi <= 45) 2 else if (rssi <= 65) 3 else 4
        val oldLevel =
            if (previousRssi <= 10) 0 else if (previousRssi <= 28) 1 else if (previousRssi <= 45) 2 else if (previousRssi <= 65) 3 else 4
        return newLevel != oldLevel
    }


}