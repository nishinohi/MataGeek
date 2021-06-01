package com.example.matageek.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.matageek.databinding.FragmentDeviceNonActivatedBinding
import com.example.matageek.dialog.DialogDeviceNameEdit
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.viewmodels.DeviceNonActivatedViewModel
import com.example.matageek.viewmodels.ScannerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

class DeviceNonActivatedFragment : Fragment() {
    private lateinit var _bind: FragmentDeviceNonActivatedBinding
    private val bind get() = _bind
    private val deviceNonActivatedViewModel: DeviceNonActivatedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _bind = FragmentDeviceNonActivatedBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        deviceNonActivatedViewModel.batteryInfo.observe(viewLifecycleOwner, {
            bind.activatedBattery.text = "$it%"
        })
        // button handler
        bind.activateButton.setOnClickListener {
            bind.activateButton.isClickable = false
            CoroutineScope(Job()).launch {
                deviceNonActivatedViewModel.inProgress()
                try {
                    deviceNonActivatedViewModel.sendEnrollmentBroadcastAppStart()
                    Log.d("MATAG", "succ enrolled node ids: ${deviceNonActivatedViewModel.enrolledNodeId}")
                } catch (e: TimeoutCancellationException) {
                    Log.d("MATAG", "fail enrolled node ids: ${deviceNonActivatedViewModel.enrolledNodeId}")
                } finally {
                    deviceNonActivatedViewModel.endProgress()
                    bind.activateButton.isClickable = true
                }
            }
        }
        return bind.root
    }

}