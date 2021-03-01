package com.example.matageek.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.matageek.profile.callback.MeshAccessDataCallback
import com.example.matageek.profile.callback.EncryptionState
import com.example.matageek.profile.FruityDataEncryptAndSplit
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import java.util.*
import javax.crypto.SecretKey

class MataGeekBleManager(context: Context) :
    ObservableBleManager(context) {

    /** MeshAccessService Characteristics */
    private lateinit var meshAccessService: BluetoothGattService

    override fun getGattCallback(): BleManagerGattCallback {
        return MataGeekBleManagerGattCallback()
    }

    private val meshAccessDataCallback: MeshAccessDataCallback =
        object : MeshAccessDataCallback() {
            override fun sendPacket(
                data: Data, encryptionNonce: Array<Int>?, encryptionKey: SecretKey?,
            ) {
                writeCharacteristic(this.maRxCharacteristic, data)
                    .split(FruityDataEncryptAndSplit(encryptionNonce, encryptionKey)).with(this)
                    .enqueue()
            }

            override fun initialize() {
                encryptionState.postValue(EncryptionState.NOT_ENCRYPTED)
                setNotificationCallback(maTxCharacteristic).with(this)
                enableNotifications(maTxCharacteristic).with(this).enqueue()
            }

        }

    fun startEncryptionHandshake() {
        meshAccessDataCallback.startEncryptionHandshake()
    }

    private inner class MataGeekBleManagerGattCallback : BleManagerGattCallback() {
        override fun initialize() {
            meshAccessDataCallback.initialize()
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val maService: BluetoothGattService? = gatt.getService(MA_UUID_SERVICE)
            val maRxCharacteristic = maService?.getCharacteristic(MA_UUID_RX_CHAR)
            val maTxCharacteristic = maService?.getCharacteristic(MA_UUID_TX_CHAR)
            if (maRxCharacteristic == null || maTxCharacteristic == null) return false
            meshAccessService = maService

            meshAccessDataCallback.maTxCharacteristic = maTxCharacteristic
            meshAccessDataCallback.maRxCharacteristic = maRxCharacteristic
            return (maRxCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0
        }

        override fun onDeviceDisconnected() {
            Log.d("MATAG", "onDeviceDisconnected: ")
        }

    }

    companion object {
        const val NODE_ID: Short = 32000

        /** MeshAccessService UUID */
        val MA_UUID_SERVICE: UUID = UUID.fromString("00000001-acce-423c-93fd-0c07a0051858")

        /** RX characteristic UUID (use for send packet to peripheral) */
        val MA_UUID_RX_CHAR: UUID = UUID.fromString("00000002-acce-423c-93fd-0c07a0051858")

        /** TX characteristic UUID (use for receive packet from peripheral) */
        val MA_UUID_TX_CHAR: UUID = UUID.fromString("00000003-acce-423c-93fd-0c07a0051858")

    }

}