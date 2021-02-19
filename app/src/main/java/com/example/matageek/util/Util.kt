package com.example.matageek.util

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.provider.Settings

class Util {
    companion object {
        fun isBleEnabled(context: Context): Boolean {
            return (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled
        }

        fun isLocationEnabled(context: Context) {
            // is marshmallow Or Above
        }
    }
}