package com.example.matageek.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.matageek.databinding.FragmentDeviceActivatedBinding
import com.example.matageek.fruity.module.MatageekModule
import com.example.matageek.viewmodels.DeviceActivatedViewModel

class DeviceActivatedFragment : Fragment() {
    private lateinit var _bind: FragmentDeviceActivatedBinding
    private val bind get() = _bind
    private val deviceActivatedViewModel: DeviceActivatedViewModel by activityViewModels()
    var listener: OnDeviceInfoUpdatedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _bind = FragmentDeviceActivatedBinding.inflate(inflater, container, false)
        deviceActivatedViewModel.deviceName.observe(viewLifecycleOwner, {
            bind.activatedDeviceName.text = it
        })
        deviceActivatedViewModel.battery.observe(viewLifecycleOwner, {
            bind.activatedBattery.text = "$it%"
        })
        deviceActivatedViewModel.clusterSize.observe(viewLifecycleOwner, {
            bind.activatedClusterSize.text = it.toString()
        })
        deviceActivatedViewModel.trapState.observe(viewLifecycleOwner, {
            bind.activatedTrapState.text = if (it) "Fired" else "Not Fired"
        })
        deviceActivatedViewModel.mode.observe(viewLifecycleOwner, {
            bind.matageekMode.text =
                if (it == MatageekModule.MatageekMode.SETUP) "SETUP" else "DETECT"
            bind.modeChangeButton.text =
                if (it == MatageekModule.MatageekMode.SETUP) "START DETECT" else "STOP DETECT"
        })
        // button handler
        bind.modeChangeButton.setOnClickListener {
            deviceActivatedViewModel.sendMatageekModeChangeMessage()
        }
        listener?.onDeviceInfoUpdated()

        return bind.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnDeviceInfoUpdatedListener
        if (listener == null) throw ClassCastException("$context must implement OnDeviceInfoUpdatedListener")
    }

    interface OnDeviceInfoUpdatedListener {
        fun onDeviceInfoUpdated()
    }

}