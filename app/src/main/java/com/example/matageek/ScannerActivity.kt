package com.example.matageek

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.matageek.adapter.DevicesAdapter
import com.example.matageek.databinding.ActivityScannerBinding
import com.example.matageek.viewmodels.ScannerViewModel

class ScannerActivity : AppCompatActivity() {
    //    private lateinit var scannerViewModel: ScannerViewModel
    val scannerViewModel: ScannerViewModel by viewModels()
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
            switchContentsVisibility(
                it, scannerViewModel.isGpsEnable(), scannerViewModel.getDeniedPermission())
            switchScanner(
                it, scannerViewModel.isGpsEnable(), scannerViewModel.getDeniedPermission())
        }
        scannerViewModel.gpsState.observe(this, {
            switchContentsVisibility(
                scannerViewModel.isBleEnable(), it, scannerViewModel.getDeniedPermission())
            switchScanner(
                scannerViewModel.isBleEnable(), it, scannerViewModel.getDeniedPermission())
        })
        scannerViewModel.deniedPermissionState.observe(this) {
            switchContentsVisibility(
                scannerViewModel.isBleEnable(), scannerViewModel.isGpsEnable(), it)
            switchScanner(scannerViewModel.isBleEnable(), scannerViewModel.isGpsEnable(), it)
        }
        scannerViewModel.scannerLiveData.observe(this, {
//            scannerViewModel.startScan()
        })
        // permission check
        permissionLauncher = registerPermissionLauncher()
        scannerViewModel.updateDeniedPermission(checkDeniedPermission(requestPermissions))
        // set on button handler
        val bleEnableLauncher = registerLaunchSubActivity(
            { scannerViewModel.enableBle() }, { scannerViewModel.disableBle() })
        bind.settingDisable.actionEnableBluetooth.setOnClickListener {
            bleEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        bind.permissionDeniedLayout.actionEnableBluetoothPermission.setOnClickListener {
            scannerViewModel.getDeniedPermission()?.let {
                permissionLauncher.launch(it.toTypedArray())
            }
        }
        val gpsEnableLauncher = registerLaunchSubActivity(
            { scannerViewModel.enableGps() }, { scannerViewModel.disableGps() }
        )
        bind.settingDisable.actionEnableGps.setOnClickListener {
            gpsEnableLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        bind.permissionDeniedLayout.actionEnableGpsPermission.setOnClickListener {
            scannerViewModel.getDeniedPermission()?.let {
                permissionLauncher.launch(it.toTypedArray())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scannerViewModel.updateGpsState()
        scannerViewModel.updateBleState()
    }

    private fun switchContentsVisibility(
        isBleEnabled: Boolean, isGpsEnabled: Boolean, deniedPermissions: List<String>?,
    ) {
        if (!isBleEnabled || !isGpsEnabled) {
            bind.scannedDeviceList.visibility = View.GONE
            bind.permissionDeniedLayout.root.visibility = View.GONE
            bind.settingDisable.root.visibility = View.VISIBLE
            bind.settingDisable.bluetoothGroup.visibility =
                if (!isBleEnabled) View.VISIBLE else View.GONE
            // ble is priority
            bind.settingDisable.gpsGroup.visibility =
                if (!isGpsEnabled && isBleEnabled) View.VISIBLE else View.GONE
            return
        }
        if (deniedPermissions?.isNotEmpty() == true) {
            bind.scannedDeviceList.visibility = View.GONE
            bind.settingDisable.root.visibility = View.GONE
            bind.permissionDeniedLayout.root.visibility = View.VISIBLE
            if (deniedPermissions.find { it == Manifest.permission.ACCESS_FINE_LOCATION } != null) {
                bind.permissionDeniedLayout.gpsPermissionGroup.visibility = View.VISIBLE
                bind.permissionDeniedLayout.bluetoothPermissionGroup.visibility = View.GONE
                return
            }
            if (deniedPermissions.find {
                    it == Manifest.permission.BLUETOOTH || it == Manifest.permission.BLUETOOTH_ADMIN
                } != null) {
                bind.permissionDeniedLayout.gpsPermissionGroup.visibility = View.GONE
                bind.permissionDeniedLayout.bluetoothPermissionGroup.visibility = View.VISIBLE
                return
            }
            return
        }
        bind.scannedDeviceList.visibility = View.VISIBLE
        bind.settingDisable.root.visibility = View.GONE
        bind.permissionDeniedLayout.root.visibility = View.GONE
    }

    private fun registerLaunchSubActivity(
        resultOkCallback: () -> Unit,
        resultCanceledCallback: () -> Unit,
    ): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                resultOkCallback()
            } else {
                resultCanceledCallback()
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

    private fun switchScanner(
        isBleEnabled: Boolean, isGpsEnabled: Boolean, deniedPermissions: List<String>?,
    ) {
        if (isBleEnabled && isGpsEnabled && deniedPermissions?.isEmpty() == true) {
            scannerViewModel.startScan()
        } else {
            scannerViewModel.stopScan()
        }
    }

}