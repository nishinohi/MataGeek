package com.example.matageek.util

import android.bluetooth.BluetoothManager
import android.content.Context

class Util {
    companion object {
        fun isBleEnabled(context: Context): Boolean {
            return (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled
        }

        fun isLocationEnabled(context: Context) {
            // is marshmallow Or Above
        }

        fun xorBytes(
            src: ByteArray, offsetSrc: Int,
            xor: ByteArray, offsetXor: Int, length: Int,
        ): ByteArray {
            val dist = ByteArray(length)
            for (ii in 0 until length) {
                dist[ii] =
                    ((src[ii + offsetSrc]).toInt() xor (xor[ii + offsetXor]).toInt()).toByte()
            }
            return dist
        }

    }
}