package com.example.matageek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.matageek.R
import com.example.matageek.databinding.DeviceItemBinding
import com.example.matageek.databinding.DeviceItemBinding.*

class DevicesAdapter :
    ListAdapter<DiscoveredDevice, DevicesAdapter.DeviceViewHolder>(DiscoveredDeviceDiffCallback) {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val _binding: DeviceItemBinding = bind(itemView)
        val binding get() = _binding
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val discoveredDevice: DiscoveredDevice = getItem(position)
        holder.binding.deviceName.text = discoveredDevice.name ?: "Unknown Device"
        holder.binding.deviceAddress.text = discoveredDevice.device.address
        // TODO RSSI
    }

    object DiscoveredDeviceDiffCallback : DiffUtil.ItemCallback<DiscoveredDevice>() {
        override fun areItemsTheSame(
            oldItem: DiscoveredDevice, newItem: DiscoveredDevice,
        ): Boolean {
            return oldItem.device.address == newItem.device.address
        }

        override fun areContentsTheSame(
            oldItem: DiscoveredDevice, newItem: DiscoveredDevice,
        ): Boolean {
            return oldItem.device.address == newItem.device.address
        }

    }

}