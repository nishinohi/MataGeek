package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.matageek.fruity.hal.BleGapAdType
import com.example.matageek.util.Util
import no.nordicsemi.android.ble.exception.BluetoothDisabledException

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val bleAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val manager = application.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }
    private val scanner: BluetoothLeScanner = bleAdapter.bluetoothLeScanner
    val scannerLiveData: ScannerStateLiveData = ScannerStateLiveData()
    val bluetoothState: MutableLiveData<Boolean> = MutableLiveData()
    val devicesLiveData: DevicesLiveData = DevicesLiveData(true, false)
    val deniedPermissionState: MutableLiveData<List<String>> = MutableLiveData()

    init {
        bluetoothState.postValue(bleAdapter.isEnabled)
    }

    fun refresh() {
        scannerLiveData.refresh()
    }

    fun startScan() {
        if (scannerLiveData.isScanning) return
        val scanSettings = ScanSettings.Builder().apply {
            setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            setReportDelay(500)
        }.build()

//        handler.postDelayed({
//            scannerLiveData.scanningStopped()
//            scanner?.stopScan(onScanResult)
//        }, SCAN_PERIOD)

        scanner.startScan(onScanResult)
        scannerLiveData.scanningStarted()
    }

    fun stopScan() {
        if (scannerLiveData.isScanning && BluetoothAdapter.getDefaultAdapter().isEnabled) {
            scanner.stopScan(onScanResult)
        }
    }

    fun enableBle() {
        bluetoothState.postValue(true)
    }

    fun disableBle() {
        bluetoothState.postValue(false)
    }

    fun isBleEnable(): Boolean {
        return bluetoothState.value == true
    }

    fun updateDeniedPermission(deniedPermissions: List<String>) {
        deniedPermissionState.postValue(deniedPermissions)
    }

    /**
     * null -> permission has not been checked
     */
    fun getDeniedPermission(): List<String>? {
        return deniedPermissionState.value
    }

    private val onScanResult: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result == null) return
            devicesLiveData.deviceDiscovered(result)
            devicesLiveData.applyFilter()
            scannerLiveData.recordFound()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("SCAN", "onScanFailed: ")
            scannerLiveData.scanningStopped()
        }
    }

    companion object {
        private const val PREFS_ID = "scanner_filter"
        private const val PREFS_FILTER_UUID_REQUIRED = "filter_uuid"
        private const val PREFS_FILTER_NEARBY_ONLY = "filter_nearby"
        private const val SCAN_PERIOD = 10000L
    }
}