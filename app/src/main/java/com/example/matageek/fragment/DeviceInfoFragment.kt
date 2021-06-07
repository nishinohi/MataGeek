package com.example.matageek.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.example.matageek.R
import com.example.matageek.databinding.FragmentDeviceInfoBinding
import com.example.matageek.dialog.DialogDeviceNameEdit
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.MeshAccessManager
import com.example.matageek.viewmodels.AbstractDeviceConfigViewModel
import com.example.matageek.viewmodels.DeviceActivatedViewModel
import com.example.matageek.viewmodels.DeviceNonActivatedViewModel
import com.example.matageek.viewmodels.ScannerViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver

class DeviceInfoFragment : Fragment(),
    DialogDeviceNameEdit.NoticeDeviceConfigListener,
    AdapterView.OnItemSelectedListener {

    private lateinit var _bind: FragmentDeviceInfoBinding
    private val bind get() = _bind
    private val deviceActivatedViewModel: DeviceActivatedViewModel by activityViewModels()
    private val deviceNonActivatedViewModel: DeviceNonActivatedViewModel by activityViewModels()
    private val scannerViewModel: ScannerViewModel by activityViewModels()
    private lateinit var currentViewModel: AbstractDeviceConfigViewModel
    lateinit var deviceNamePreferences: SharedPreferences
    private lateinit var spinnerAdapter: ArrayAdapter<Short>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _bind = FragmentDeviceInfoBinding.inflate(inflater, container, false)
        // load setting
        deviceNamePreferences =
            requireContext().getSharedPreferences(getString(R.string.preference_device_name_key),
                Context.MODE_PRIVATE)
        // set spinner adapter
        spinnerAdapter =
            ArrayAdapter<Short>(requireContext(), android.R.layout.simple_spinner_item).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        bind.nodeIdSpinner.adapter = spinnerAdapter
        bind.nodeIdSpinner.onItemSelectedListener = this
        // transaction
        val fragmentTransaction =
            requireActivity().supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.device_content_fragment,
            if (scannerViewModel.selectedDevice.enrolled) DeviceActivatedFragment() else DeviceNonActivatedFragment())
        fragmentTransaction.commit()
        // connect device
        currentViewModel =
            if (isActivated()) deviceActivatedViewModel else deviceNonActivatedViewModel
        currentViewModel.connect(scannerViewModel.selectedDevice)
        // set observer
        currentViewModel.deviceName.observe(viewLifecycleOwner, {
            bind.activatedDeviceName.text = it
        })
        currentViewModel.connectionState.observe(viewLifecycleOwner, {
            onConnectionUpdated(it)
        })
        currentViewModel.handShakeState.observe(viewLifecycleOwner, {
            onHandShakeUpdated(it)
        })
        currentViewModel.clusterSize.observe(viewLifecycleOwner, {
            bind.activatedClusterSize.text = it.toString()
            currentViewModel.updateMeshGraph()
        })
        currentViewModel.nodeIdList.observe(viewLifecycleOwner, {
            spinnerAdapter.clear()
            spinnerAdapter.addAll(it.sorted())
            spinnerAdapter.notifyDataSetChanged()
            it.sorted().forEachIndexed { index, nodeId ->
                if (nodeId == currentViewModel.displayNodeId) {
                    bind.nodeIdSpinner.setSelection(index)
                }
            }
        })
        bind.icActivatedDeviceNameEdit.setOnClickListener {
            DialogDeviceNameEdit().show(childFragmentManager, "test")
        }
        currentViewModel.progressState.observe(viewLifecycleOwner, {
            requireActivity().findViewById<ProgressBar>(R.id.progress_bar).visibility =
                if (it) View.VISIBLE else View.INVISIBLE
        })
        currentViewModel.endProgress()

        return bind.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            currentViewModel.disconnect()
            isEnabled = false
        }
    }

    private fun isActivated(): Boolean {
        return scannerViewModel.selectedDevice.enrolled
    }

    private fun onConnectionUpdated(connectionState: ConnectionState) {
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
                currentViewModel.startHandShake()
            }
            ConnectionState.State.DISCONNECTING -> {
                Log.d("MATAG", "onCreate: DISCONNECTING")
            }
            ConnectionState.State.DISCONNECTED -> {
                if ((connectionState as ConnectionState.Disconnected).reason != ConnectionObserver.REASON_UNKNOWN) {
                    view?.findNavController()
                        ?.navigate(DeviceInfoFragmentDirections.actionDeviceInfoFragmentToScannerFragment())
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
                currentViewModel.displayBleAddr = scannerViewModel.selectedDevice.device.address
                bind.connectingGroup.visibility = View.GONE
                bind.deviceConfigGroup.visibility = View.VISIBLE
                updateConnectionInfo()
            }
            else -> throw Exception("Unknown Handshake state")
        }
    }

    private fun showConnectingStatus(stringId: Int) {
        bind.connectingGroup.visibility = View.VISIBLE
        bind.connectingText.setText(stringId)
    }

    private fun updateConnectionInfo() {
        currentViewModel.updateDisplayDeviceInfo(DeviceInfo(
            currentViewModel.displayNodeId, null, null, null,
            deviceNamePreferences.getString(scannerViewModel.selectedDevice.device.address,
                "Unknown Device")))
        currentViewModel.updateDeviceInfo(currentViewModel.displayNodeId)
        currentViewModel.updateMatageekStatus(currentViewModel.displayNodeId)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, deviceName: String) {
        deviceNamePreferences.edit().putString(currentViewModel.displayBleAddr, deviceName)
            .apply()
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