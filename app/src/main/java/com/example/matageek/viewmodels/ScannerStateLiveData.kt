package com.example.matageek.viewmodels

import androidx.lifecycle.LiveData

class ScannerStateLiveData(
    private var locationEnabled: Boolean,
    private var bluetoothEnabled: Boolean,
) : LiveData<ScannerStateLiveData>() {
    var isScanning: Boolean

    init {
        this.isScanning = false
        postValue(this)
    }

    /* package */
    fun scanningStarted() {
        isScanning = true
        postValue(this)
    }

    /* package */
    fun scanningStopped() {
        isScanning = false
        postValue(this)
    }

    /* package */
    fun bluetoothEnabled() {
        bluetoothEnabled = true
        postValue(this)
    }

    fun refresh() {
        postValue(this)
    }

}