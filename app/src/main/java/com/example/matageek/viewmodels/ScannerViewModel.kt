package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.matageek.util.Util

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner: BluetoothLeScanner? = adapter?.bluetoothLeScanner
    val preferences: SharedPreferences =
        application.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE)
    private val handler = Handler()

    val scannerLiveData: ScannerStateLiveData =
        ScannerStateLiveData(true, Util.isBleEnabled(application))

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

        scanner?.startScan(onScanResult)
        scannerLiveData.scanningStarted()
    }

    fun stopScan() {
        if (scannerLiveData.isScanning && BluetoothAdapter.getDefaultAdapter().isEnabled) {
            scanner?.stopScan(onScanResult)
        }
    }

    val onScanResult: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.d("SCAN", "onScanResult: $result")
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