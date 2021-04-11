package com.example.matageek.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.matageek.databinding.FragmentDeviceNonActivatedBinding
import com.example.matageek.dialog.DialogDeviceNameEdit
import com.example.matageek.viewmodels.DeviceNonActivatedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DeviceNonActivatedFragment : Fragment() {
    private lateinit var _bind: FragmentDeviceNonActivatedBinding
    private val bind get() = _bind
    private val deviceNonActivatedViewModel: DeviceNonActivatedViewModel by activityViewModels()
    var listener: DeviceActivatedFragment.OnDeviceInfoUpdatedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _bind = FragmentDeviceNonActivatedBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        deviceNonActivatedViewModel.deviceName.observe(viewLifecycleOwner, {
            bind.activatedDeviceName.text = it
        })
        deviceNonActivatedViewModel.battery.observe(viewLifecycleOwner, {
            bind.activatedBattery.text = "$it%"
        })
        deviceNonActivatedViewModel.clusterSize.observe(viewLifecycleOwner, {
            bind.activatedClusterSize.text = it.toString()
        })
        // button handler
        bind.activateButton.setOnClickListener {
            bind.activateButton.isClickable = false
            CoroutineScope(Job()).launch {
                deviceNonActivatedViewModel.inProgress()
                try {
                    deviceNonActivatedViewModel.sendEnrollmentBroadcastAppStart()
                } finally {
                    deviceNonActivatedViewModel.endProgress()
                    bind.activateButton.isClickable = true
                }
            }
        }
        bind.icActivatedDeviceNameEdit.setOnClickListener {
            DialogDeviceNameEdit().show(childFragmentManager, "test")
        }

        listener?.onDeviceInfoUpdated()

        return bind.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? DeviceActivatedFragment.OnDeviceInfoUpdatedListener
        if (listener == null) throw ClassCastException("$context must implement OnDeviceInfoUpdatedListener")
    }

    interface OnDeviceInfoUpdatedListener {
        fun onDeviceInfoUpdated()
    }

}