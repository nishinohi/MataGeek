package com.example.matageek.viewmodels

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.manager.MataGeekBleManager

class DevicesLiveData(
    private val filterUuidRequired: Boolean,
    private val filterNearbyOnly: Boolean,
) :
    LiveData<MutableList<DiscoveredDevice>>() {

    private val discoveredDevices: MutableList<DiscoveredDevice> = mutableListOf()
    var filteredDevices: MutableList<DiscoveredDevice> = mutableListOf()

    // TODO
    fun applyFilter(): Boolean {
        filteredDevices = discoveredDevices.filter { discoveredDevice ->
            this.filterNearby(discoveredDevice.lastScanResult, FILTER_RSSI) &&
                    this.filterServiceUuid(discoveredDevice.lastScanResult)
        }.toMutableList()
        postValue(filteredDevices)
        return filteredDevices.isNotEmpty()
    }

    @Synchronized
    fun deviceDiscovered(scanResult: ScanResult) {
        val existedDevice: List<DiscoveredDevice> =
            discoveredDevices.filter { discoveredDevice ->
                discoveredDevice.device.address.equals(scanResult.device.address)
            }
        val newDevice: DiscoveredDevice =
            if (existedDevice.isEmpty()) DiscoveredDevice(scanResult) else existedDevice[0]
        if (existedDevice.isEmpty()) discoveredDevices.add(newDevice)
        newDevice.update(scanResult)
    }

    private fun filterServiceUuid(scanResult: ScanResult): Boolean {
        if (!this.filterUuidRequired) return true

        // sometimes scan result doesn't contain service uuid
        // so, once discovered device is excluded for filtering
        if (filteredDevices.find {
                it.device.address.equals(scanResult.device.address)
            } != null) return true

        val scanServiceUuids: MutableList<ParcelUuid> = scanResult.scanRecord?.serviceUuids
            ?: return false
        val maServiceUuid =
            scanServiceUuids.find { parcelUuid ->
                return (parcelUuid.uuid.toString().substring(0, 8)
                    .compareTo(MataGeekBleManager.MESH_SERVICE_DATA_SERVICE_UUID16.toString()
                        .substring(0, 8)) == 0)
            }

        return maServiceUuid != null
    }

    private fun filterNearby(scanResult: ScanResult, rssi: Int): Boolean {
        return !this.filterNearbyOnly || scanResult.rssi > rssi
    }

    companion object {
        private const val FILTER_RSSI = -50 // [dBm]
    }
}