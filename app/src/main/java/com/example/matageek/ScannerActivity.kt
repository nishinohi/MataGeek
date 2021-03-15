package com.example.matageek

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.example.matageek.adapter.DevicesAdapter
import com.example.matageek.databinding.ActivityScannerBinding
import com.example.matageek.viewmodels.ScannerStateLiveData
import com.example.matageek.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity() {
    private lateinit var scannerViewModel: ScannerViewModel
    private lateinit var _binding: ActivityScannerBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scannerViewModel: ScannerViewModel by viewModels()
        this.scannerViewModel = scannerViewModel
        scannerViewModel.scannerLiveData.observe(this, this::startScan)
        val adapter = DevicesAdapter {
            Log.d("MATAG", "onCreate: $it")
            Intent(this,
                if (it.enrolled) DeviceManageActivity::class.java else DeviceConfigActivity::class.java).apply {
                putExtra(DeviceConfigActivity.EXTRA_DEVICE, it)
                startActivity(this)
            }
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