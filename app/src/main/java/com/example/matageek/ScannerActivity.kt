package com.example.matageek

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
        checkPermission()

        val adapter = DevicesAdapter {
            Intent(this, DeviceConfigActivity::class.java).apply {
                putExtra(DeviceConfigActivity.EXTRA_DEVICE, it)
                startActivity(this)
            }
        }
        binding.scannedDeviceList.adapter = adapter
        scannerViewModel.devicesLiveData.observe(this) {
            it?.let {
                adapter.submitList(it)
            }
        }
    }

    private fun checkPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
                permission.forEach {
                    if (it.value == null || !it.value) return@registerForActivityResult
                }
                startScanner()
            }

        val requestPermissions = arrayListOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        val deniedPermissions = mutableListOf<String>()
        requestPermissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(it)
            }
        }
        if (deniedPermissions.isEmpty()) {
            startScanner()
            return
        }
        requestPermissionLauncher.launch(deniedPermissions.toTypedArray())
    }

    private fun startScanner() {
        scannerViewModel.scannerLiveData.observe(this, {
            scannerViewModel.startScan()
        })
    }
}