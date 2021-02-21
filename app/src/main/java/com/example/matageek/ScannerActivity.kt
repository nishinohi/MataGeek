package com.example.matageek

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matageek.adapter.DevicesAdapter
import com.example.matageek.databinding.ActivityScannerBinding
import com.example.matageek.util.Util
import com.example.matageek.viewmodels.ScannerStateLiveData
import com.example.matageek.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity() {
    private lateinit var scannerViewModel: ScannerViewModel
    private lateinit var _binding: ActivityScannerBinding
    val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_scanner)
        val isEnable = Util.isBleEnabled(this);
        Log.d("MATAG", "is ble enable:${isEnable}")
        val scannerViewModel: ScannerViewModel by viewModels()
        this.scannerViewModel = scannerViewModel
        scannerViewModel.scannerLiveData.observe(this, this::startScan)
        val recyclerView = findViewById<RecyclerView>(R.id.scanned_device_list)
//        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.scannedDeviceList.addItemDecoration(DividerItemDecoration(this,
            DividerItemDecoration.VERTICAL))
        binding.scannedDeviceList.adapter = DevicesAdapter(scannerViewModel.devicesLiveData, this)
    }

    private fun startScan(scannerStateLiveData: ScannerStateLiveData) {
        this.scannerViewModel.startScan()
        Log.d("SCAN", "startScan: $scannerStateLiveData")
    }
}