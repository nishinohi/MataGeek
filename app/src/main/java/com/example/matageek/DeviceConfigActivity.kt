package com.example.matageek

import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.DialogFragment
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.databinding.ActivityDeviceConfigBinding
import com.example.matageek.dialog.DialogDeviceNameEdit
import com.example.matageek.viewmodels.DeviceConfigViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver

class DeviceConfigActivity : AppCompatActivity(), DialogDeviceNameEdit.NoticeDeviceConfigListener {
    private lateinit var _bind: ActivityDeviceConfigBinding
    private val bind get() = _bind
    private lateinit var deviceConfigViewModel: DeviceConfigViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bind = ActivityDeviceConfigBinding.inflate(layoutInflater)
        setContentView(bind.root)
        val discoveredDevice: DiscoveredDevice = intent.getParcelableExtra(EXTRA_DEVICE)
            ?: throw Resources.NotFoundException("device")
        setSupportActionBar(bind.deviceConfigToolBar)
        supportActionBar?.title = "Mesh"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val deviceConfigViewModel: DeviceConfigViewModel by viewModels()
        this.deviceConfigViewModel = deviceConfigViewModel
        deviceConfigViewModel.connect(discoveredDevice.device)
        deviceConfigViewModel.connectionState.observe(this, {
            onConnectionUpdated(it)
        })
        deviceConfigViewModel.deviceName.observe(this, {
            bind.deviceName.text = it
        })
        deviceConfigViewModel.clusterSize.observe(this, {
            bind.clusterSize.text = it.toString()
        })
        deviceConfigViewModel.battery.observe(this, {
            bind.battery.text = "$it%"
        })

        // set Activate Button handler
        bind.activate.setOnClickListener {
            deviceConfigViewModel.sendGetStatusMessage()
        }
        bind.icDeviceNameEdit.setOnClickListener {
            val deviceNameConfigDialog = DialogDeviceNameEdit()
            deviceNameConfigDialog.show(supportFragmentManager, "test")
        }
    }

    override fun onBackPressed() {
        deviceConfigViewModel.disconnect()
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // pressed action bar back button
            android.R.id.home -> {
                deviceConfigViewModel.disconnect()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onConnectionUpdated(
        connectionState
        : ConnectionState,
    ) {
        when (connectionState.state) {
            ConnectionState.State.CONNECTING -> {
                showConnectingStatus(R.string.status_connecting)
                Log.d("MATAG", "onCreate: CONNECTING")
            }
            ConnectionState.State.INITIALIZING -> {
                showConnectingStatus(R.string.status_initializing)
                Log.d("MATAG", "onCreate: INITIALIZING")
            }
            ConnectionState.State.READY -> {
                bind.deviceConfigGroup.visibility = View.VISIBLE
                bind.connectingGroup.visibility = View.GONE
                deviceConfigViewModel.startHandShake()
                deviceConfigViewModel.loadDeviceName()
            }
            ConnectionState.State.DISCONNECTING -> {
                Log.d("MATAG", "onCreate: DISCONNECTING")
            }
            ConnectionState.State.DISCONNECTED -> {
                if ((connectionState as ConnectionState.Disconnected).reason == ConnectionObserver.REASON_TERMINATE_PEER_USER) {
                    // TODO delete history
                    Intent(this, ScannerActivity::class.java).apply {
                        startActivity(this)
                    }
                }
                Log.d("MATAG",
                    "onCreate: DISCONNECTING reason ${(connectionState as ConnectionState.Disconnected).reason}")
            }
        }
    }

    private fun showConnectingStatus(stringId: Int) {
        bind.deviceConfigGroup.visibility = View.GONE
        bind.connectingGroup.visibility = View.VISIBLE
        bind.connectingText.setText(stringId)
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, deviceName: String) {
        if (deviceName.isEmpty()) return
        deviceConfigViewModel.setDeviceName(deviceName)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }
}