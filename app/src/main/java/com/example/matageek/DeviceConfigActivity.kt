package com.example.matageek

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.databinding.ActivityDeviceConfigBinding
import com.example.matageek.viewmodels.DeviceConfigViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class DeviceConfigActivity : AppCompatActivity() {
    private lateinit var _bind: ActivityDeviceConfigBinding
    private val bind get() = _bind

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
        deviceConfigViewModel.connect(discoveredDevice.device)
        deviceConfigViewModel.connectionState.observe(this, {
            when (it.state) {
                ConnectionState.State.CONNECTING -> {
                    Log.d("MATAG", "onCreate: CONNECTING")
                }
                ConnectionState.State.INITIALIZING -> {
                    Log.d("MATAG", "onCreate: INITIALIZING")
                }
                ConnectionState.State.READY -> {
                    deviceConfigViewModel.startHandShake()
                }
                ConnectionState.State.DISCONNECTING -> {
                    Log.d("MATAG", "onCreate: DISCONNECTING")
                }
                ConnectionState.State.DISCONNECTED -> {
                    Log.d("MATAG", "onCreate: DISCONNECTED")
                }
            }
        })
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }
}