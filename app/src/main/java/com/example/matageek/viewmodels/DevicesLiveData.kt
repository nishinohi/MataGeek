package com.example.matageek.viewmodels

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.matageek.adapter.DiscoveredDevice

class DevicesLiveData(private val filterUuidRequired: Boolean, private val filterNearbyOnly: Boolean) :
    LiveData<MutableList<DiscoveredDevice>>() {

    private val discoveredDevices: MutableList<DiscoveredDevice> = mutableListOf()
    var filteredDevices: MutableList<DiscoveredDevice> = mutableListOf()

    // TODO
    fun applyFilter(): Boolean {
        val temp: List<DiscoveredDevice> = discoveredDevices.filter { discoveredDevice ->
            this.filterNearby(discoveredDevice.lastScanResult, FILTER_RSSI) &&
                    this.filterServiceUuid(discoveredDevice.lastScanResult)
        }
        filteredDevices = temp.toMutableList()
        postValue(filteredDevices)
        Log.d("SCAN", "applyFilter: $filteredDevices")
        return filteredDevices.isNotEmpty()
    }

    @Synchronized
    fun deviceDiscovered(scanResult: ScanResult): Boolean {
        val existedDevice: List<DiscoveredDevice> =
            discoveredDevices.filter { discoveredDevice ->
                discoveredDevice.device.address.equals(scanResult.device.address)
            }
        val newDevice: DiscoveredDevice =
            if (existedDevice.isEmpty()) DiscoveredDevice(scanResult) else existedDevice[0]
        if (existedDevice.isEmpty()) discoveredDevices.add(newDevice)
        newDevice.update(scanResult)
        // TODO apply UUID filter to scan result
        return filterNearby(scanResult, FILTER_RSSI)
    }

    fun filterServiceUuid(scanResult: ScanResult): Boolean {
        val temp: MutableList<ParcelUuid>? = scanResult.scanRecord?.serviceUuids
        // TODO
        return !this.filterUuidRequired || true
    }

    fun filterNearby(scanResult: ScanResult, rssi: Int): Boolean {
        return !this.filterNearbyOnly || scanResult.rssi > rssi
    }

    companion object {
        private const val FILTER_RSSI = -50 // [dBm]
    }
}