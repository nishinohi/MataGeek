package com.example.matageek

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.example.matageek.util.Util
import com.example.matageek.viewmodels.ScannerStateLiveData
import com.example.matageek.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity() {
    private lateinit var scannerViewModel: ScannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        val isEnable = Util.isBleEnabled(this);
        Log.d("MATAG", "is ble enable:${isEnable}")
        val scannerViewModel: ScannerViewModel by viewModels()
        this.scannerViewModel = scannerViewModel
        scannerViewModel.scannerLiveData.observe(this, this::startScan)
    }

    private fun startScan(scannerStateLiveData: ScannerStateLiveData){
        this.scannerViewModel.startScan()
        Log.d("SCAN", "startScan: $scannerStateLiveData")
    }
}