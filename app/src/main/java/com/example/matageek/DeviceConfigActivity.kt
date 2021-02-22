package com.example.matageek

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.matageek.adapter.DiscoveredDevice

class DeviceConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_config)
        val device: DiscoveredDevice? = intent.getParcelableExtra(EXTRA_DEVICE)
        Log.d("MATAG", "onCreate: ${device?.name}")
    }

    companion object {
        const val EXTRA_DEVICE: String = "no.nordicsemi.android.blinky.EXTRA_DEVICE"
    }
}