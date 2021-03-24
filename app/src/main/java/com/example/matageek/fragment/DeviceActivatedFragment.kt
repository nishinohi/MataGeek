package com.example.matageek.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.matageek.R
import com.example.matageek.databinding.FragmentDeviceActivatedBinding
import com.example.matageek.dialog.DialogDeviceNameEdit
import com.example.matageek.fruity.module.MatageekModule
import com.example.matageek.viewmodels.DeviceActivatedViewModel
import kotlinx.coroutines.*

class DeviceActivatedFragment : Fragment() {
    private lateinit var _bind: FragmentDeviceActivatedBinding
    private val bind get() = _bind
    private val deviceActivatedViewModel: DeviceActivatedViewModel by activityViewModels()
    var listener: OnDeviceInfoUpdatedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
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
            bind.icActivatedTrapState.drawable.setTint(ContextCompat.getColor(requireContext(),
                if (it) R.color.trap_fired else R.color.trap_not_fired))
        })
        deviceActivatedViewModel.mode.observe(viewLifecycleOwner, {
            bind.matageekMode.text =
                if (it == MatageekModule.MatageekMode.SETUP) "SETUP" else "DETECT"
            bind.modeChangeButton.text =
                if (it == MatageekModule.MatageekMode.SETUP) "START DETECT" else "STOP DETECT"
            bind.modeChangeButton.isClickable = true
            bind.modeChangeButton.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.button_active))
        })
        // button handler
        bind.icActivatedDeviceNameEdit.setOnClickListener {
            val deviceNameEdit = DialogDeviceNameEdit()
            deviceNameEdit.show(childFragmentManager, "test")
        }
        bind.modeChangeButton.setOnClickListener {
            CoroutineScope(Job()).launch {
                bind.modeChangeButton.isClickable = false
                bind.modeChangeButton.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.button_de_active))
                deviceActivatedViewModel.sendMatageekModeChangeMessage()
            }
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