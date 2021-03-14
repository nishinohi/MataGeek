package com.example.matageek

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.databinding.ActivityDeviceActivatedBinding
import com.example.matageek.fragment.DeviceActivatedFragment
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.MeshAccessManager
import com.example.matageek.viewmodels.AbstractDeviceConfigViewModel
import com.example.matageek.viewmodels.DeviceActivatedViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver

class DeviceManageActivity : AppCompatActivity(),
    DeviceActivatedFragment.OnDeviceInfoUpdatedListener {
    private lateinit var _bind: ActivityDeviceActivatedBinding
    private val bind get() = _bind
    private val deviceActivatedViewModel: DeviceActivatedViewModel by viewModels()
    lateinit var deviceNamePreferences: SharedPreferences
    private val fragment = DeviceActivatedFragment()
    private lateinit var discoveredDevice: DiscoveredDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bind = ActivityDeviceActivatedBinding.inflate(layoutInflater)
        setContentView(bind.root)
        // load setting
        deviceNamePreferences = getSharedPreferences(getString(R.string.preference_device_name_key),
            Context.MODE_PRIVATE)
        // get bundle
        discoveredDevice =
            intent.getParcelableExtra(AbstractDeviceConfigViewModel.EXTRA_DEVICE)
                ?: throw Resources.NotFoundException("device")
        // action bar enable back press
        setSupportActionBar(bind.deviceManageToolBar)
        supportActionBar?.title = "Device Manager"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // connect device
        deviceActivatedViewModel.connect(discoveredDevice)
        // set observer
        deviceActivatedViewModel.connectionState.observe(this, {
            onConnectionUpdated(it)
        })
        deviceActivatedViewModel.handShakeState.observe(this, {
            onHandShakeUpdated(it)
        })
    }

    override fun onBackPressed() {
        deviceActivatedViewModel.disconnect()
        super.onBackPressed()
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
                deviceActivatedViewModel.startHandShake()
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

    private fun onHandShakeUpdated(handShakeState: MeshAccessManager.HandShakeState) {
        when (handShakeState) {
            MeshAccessManager.HandShakeState.HANDSHAKING -> {
                showConnectingStatus(R.string.handshake_state_handshaking)
                Log.d("MATAG", "onCreate: CONNECTING")
            }
            MeshAccessManager.HandShakeState.HANDSHAKE_DONE -> {
                bind.connectingGroup.visibility = View.GONE
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.add(R.id.activated_device_fragment, fragment)
                fragmentTransaction.commit()
            }
        }
    }


    private fun showConnectingStatus(stringId: Int) {
        bind.connectingGroup.visibility = View.VISIBLE
        bind.connectingText.setText(stringId)
    }

    override fun onDeviceInfoUpdated() {
        deviceActivatedViewModel.deviceName.postValue(
            deviceNamePreferences.getString(discoveredDevice.device.address, "unknown")
        )
    }

}