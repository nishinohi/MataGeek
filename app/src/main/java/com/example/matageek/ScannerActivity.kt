package com.example.matageek

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
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

        val scannerViewModel: ScannerViewModel by viewModels()
        this.scannerViewModel = scannerViewModel
        scannerViewModel.scannerLiveData.observe(this, this::startScan)
        val adapter = DevicesAdapter {
            Log.d("MATAG", "onCreate: $it")
        }
        binding.scannedDeviceList.adapter = adapter
        scannerViewModel.devicesLiveData.observe(this, {
            it?.let {
                adapter.submitList(it)
            }
        })
    }

    private fun startScan(scannerStateLiveData: ScannerStateLiveData) {
        this.scannerViewModel.startScan()
    }
}