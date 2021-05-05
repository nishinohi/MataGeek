package com.example.matageek

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.fragment.app.DialogFragment
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.databinding.ActivityDeviceConfigBinding
import com.example.matageek.dialog.DialogDeviceNameEdit
import com.example.matageek.fragment.DeviceActivatedFragment
import com.example.matageek.fragment.DeviceNonActivatedFragment
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.MeshAccessManager
import com.example.matageek.viewmodels.AbstractDeviceConfigViewModel
import com.example.matageek.viewmodels.DeviceActivatedViewModel
import com.example.matageek.viewmodels.DeviceNonActivatedViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver

class DeviceConfigActivity : AppCompatActivity(),
    DeviceActivatedFragment.OnDeviceInfoFragmentCreatedListener,
    DialogDeviceNameEdit.NoticeDeviceConfigListener,
    AdapterView.OnItemSelectedListener {
    private lateinit var _bind: ActivityDeviceConfigBinding
    private val bind get() = _bind
    private val deviceActivatedViewModel: DeviceActivatedViewModel by viewModels()
    private val deviceNonActivatedViewModel: DeviceNonActivatedViewModel by viewModels()
    private lateinit var currentViewModel: AbstractDeviceConfigViewModel
    lateinit var deviceNamePreferences: SharedPreferences
    private lateinit var discoveredDevice: DiscoveredDevice
    private lateinit var spinnerAdapter: ArrayAdapter<Short>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bind = ActivityDeviceConfigBinding.inflate(layoutInflater)
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
        // set spinner adapter
        spinnerAdapter =
            ArrayAdapter<Short>(this, android.R.layout.simple_spinner_item).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        bind.nodeIdSpinner.adapter = spinnerAdapter
        bind.nodeIdSpinner.onItemSelectedListener = this
        // connect device
        currentViewModel =
            if (isActivated()) deviceActivatedViewModel else deviceNonActivatedViewModel
        currentViewModel.connect(discoveredDevice)
        // set observer
        currentViewModel.connectionState.observe(this, {
            onConnectionUpdated(it)
        })
        currentViewModel.handShakeState.observe(this, {
            onHandShakeUpdated(it)
        })
        currentViewModel.progressState.observe(this, {
            bind.messageProgress.visibility = if (it) View.VISIBLE else View.INVISIBLE
        })
        currentViewModel.clusterSize.observe(this, {
            bind.activatedClusterSize.text = it.toString()
            currentViewModel.updateMeshGraph()
        })
        currentViewModel.nodeIdList.observe(this, {
            spinnerAdapter.clear()
            spinnerAdapter.addAll(it.sorted())
            spinnerAdapter.notifyDataSetChanged()
            it.sorted().forEachIndexed { index, nodeId ->
                if (nodeId == currentViewModel.displayNodeId) {
                    bind.nodeIdSpinner.setSelection(index)
                }
            }
        })
    }

    private fun isActivated(): Boolean {
        return discoveredDevice.enrolled
    }

    override fun onBackPressed() {
        currentViewModel.disconnect()
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                currentViewModel.disconnect()
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
                currentViewModel.startHandShake()
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
                Log.d("MATAG", "onCreate: DISCONNECTING reason ${connectionState.reason}")
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
                currentViewModel.updateDisplayNodeIdByPartnerId()
                bind.connectingGroup.visibility = View.GONE
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.add(R.id.activated_device_fragment,
                    if (discoveredDevice.enrolled) DeviceActivatedFragment() else DeviceNonActivatedFragment())
                fragmentTransaction.commit()
            }
            else -> throw Exception("Unknown Handshake state")
        }
    }

    private fun showConnectingStatus(stringId: Int) {
        bind.connectingGroup.visibility = View.VISIBLE
        bind.connectingText.setText(stringId)
    }

    override fun onDeviceInfoFragmentCreated() {
        currentViewModel.updateDisplayDeviceInfo(DeviceInfo(
            currentViewModel.displayNodeId, null, null, null,
            deviceNamePreferences.getString(discoveredDevice.device.address, "Unknown Device")))
        currentViewModel.updateMatageekStatus(currentViewModel.displayNodeId)
        currentViewModel.updateDeviceInfo(currentViewModel.displayNodeId)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, deviceName: String) {
        deviceNamePreferences.edit().putString(discoveredDevice.device.address, deviceName).apply()
        currentViewModel.updateDisplayDeviceInfo(DeviceInfo(currentViewModel.displayNodeId,
            null, null, null, deviceName))
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        spinnerAdapter.getItem(position)?.let {
            currentViewModel.displayNodeId = it
            currentViewModel.updateMatageekStatus(it)
            currentViewModel.updateDeviceInfo(it)
            currentViewModel.updateDeviceInfo2(it)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
//        TODO("Not yet implemented")
    }


}