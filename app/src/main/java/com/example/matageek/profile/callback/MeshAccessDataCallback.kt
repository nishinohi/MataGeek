package com.example.matageek.profile.callback

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.data.Data

abstract class MeshAccessDataCallback : DataReceivedCallback, DataSentCallback {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        Log.d("MATAG", "onDataReceived: $data")
    }

}

enum class EncryptionState(private val state: Int) {
    NOT_ENCRYPTED(0),
    ENCRYPTING(1),
    ENCRYPTED(2)
}
