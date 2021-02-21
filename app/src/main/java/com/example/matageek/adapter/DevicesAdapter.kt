package com.example.matageek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.example.matageek.R
import com.example.matageek.ScannerActivity
import com.example.matageek.databinding.DeviceItemBinding
import com.example.matageek.databinding.DeviceItemBinding.*
import com.example.matageek.viewmodels.DevicesLiveData

class DevicesAdapter(private val devicesLiveData: DevicesLiveData, activity: ScannerActivity) :
    RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>() {
    private var devices: MutableList<DiscoveredDevice> = mutableListOf()

    init {
        val obj = Observer<MutableList<DiscoveredDevice>> { newDevices ->
            devices = newDevices
            notifyDataSetChanged()
        }
        devicesLiveData.observe(activity, obj)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val _binding: DeviceItemBinding = bind(itemView)
        val binding get() = _binding
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val discoveredDevice: DiscoveredDevice = devices[position]
        holder.binding.deviceName.text = devices[position].name ?: "Unknown Device"
        holder.binding.deviceAddress.text = discoveredDevice.device.address
        // TODO RSSI
    }

    override fun getItemCount(): Int {
        return devices.size
    }

}