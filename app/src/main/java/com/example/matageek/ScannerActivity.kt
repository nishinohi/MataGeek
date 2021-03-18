package com.example.matageek

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.matageek.adapter.DevicesAdapter
import com.example.matageek.databinding.ActivityScannerBinding
import com.example.matageek.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity() {
    private lateinit var scannerViewModel: ScannerViewModel
    private lateinit var _bind: ActivityScannerBinding
    private val bind get() = _bind
    private val requestPermissions: List<String> =
        arrayListOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bind = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val scannerViewModel: ScannerViewModel by viewModels()
        this.scannerViewModel = scannerViewModel

        // set recycle adapter
        val adapter = DevicesAdapter {
            Intent(this, DeviceConfigActivity::class.java).apply {
                putExtra(DeviceConfigActivity.EXTRA_DEVICE, it)
                startActivity(this)
            }
        }
        bind.scannedDeviceList.adapter = adapter
        // set live data observer
        scannerViewModel.devicesLiveData.observe(this) {
            it?.let {
                adapter.submitList(it)
            }
        }
        scannerViewModel.bluetoothState.observe(this) {
            it?.let {
                switchContentsVisibility(it, scannerViewModel.getDeniedPermission())
                switchScanner(it, scannerViewModel.getDeniedPermission())
            }
        }
        scannerViewModel.deniedPermissionState.observe(this) {
            switchContentsVisibility(scannerViewModel.isBleEnable(), it)
            switchScanner(scannerViewModel.isBleEnable(), it)
        }
        scannerViewModel.scannerLiveData.observe(this, {
//            scannerViewModel.startScan()
        })
        // permission check
        permissionLauncher = registerPermissionLauncher()
        scannerViewModel.updateDeniedPermission(checkDeniedPermission(requestPermissions))
        // set on button handler
        val bleEnableLauncher = registerBleEnableSubActivity()
        bind.bluetoothDisable.actionEnableBluetooth.setOnClickListener {
            bleEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        bind.bluetoothPermissionDenied.actionEnableBluetoothPermission.setOnClickListener {
            scannerViewModel.getDeniedPermission()?.let {
                permissionLauncher.launch(it.toTypedArray())
            }
        }
    }

    private fun switchContentsVisibility(isBleEnabled: Boolean, deniedPermissions: List<String>?) {
        if (!isBleEnabled) {
            bind.scannedDeviceList.visibility = View.GONE
            bind.bluetoothDisable.root.visibility = View.VISIBLE
            bind.bluetoothPermissionDenied.root.visibility = View.GONE
            return
        }
        if (deniedPermissions?.isNotEmpty() == true) {
            bind.scannedDeviceList.visibility = View.GONE
            bind.bluetoothDisable.root.visibility = View.GONE
            bind.bluetoothPermissionDenied.root.visibility = View.VISIBLE
            return
        }
        bind.scannedDeviceList.visibility = View.VISIBLE
        bind.bluetoothDisable.root.visibility = View.GONE
    }

    private fun registerBleEnableSubActivity(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                scannerViewModel.enableBle()
            } else {
                scannerViewModel.disableBle()
            }
        }
    }

    private fun registerPermissionLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            val deniedPermission: MutableList<String> = mutableListOf()
            permission.forEach {
                if (it.value == null || !it.value) deniedPermission.add(it.key)
            }
            scannerViewModel.updateDeniedPermission(deniedPermission)
        }
    }

    private fun checkDeniedPermission(permissions: List<String>): List<String> {
        val deniedPermissions = mutableListOf<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(it)
            }
        }
        return deniedPermissions.toList()
    }

    private fun switchScanner(isBleEnabled: Boolean, deniedPermissions: List<String>?) {
        if (isBleEnabled && deniedPermissions?.isEmpty() == true) {
            scannerViewModel.startScan()
        } else {
            scannerViewModel.stopScan()
        }
    }

}