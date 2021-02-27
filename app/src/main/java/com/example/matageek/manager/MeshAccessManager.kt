package com.example.matageek.manager

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.matageek.fruity.types.ConnPacketEncryptCustomStart
import com.example.matageek.fruity.types.FmKeyId
import com.example.matageek.profile.callback.MeshAccessDataCallback
import com.example.matageek.profile.callback.EncryptionState
import com.example.matageek.profile.FruityDataSplitter
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import java.util.*

class MeshAccessManager(context: Context) :
    ObservableBleManager(context) {

    /** MeshAccessService Characteristics */
    private val nodeId: Short = 32000
    private lateinit var meshAccessService: BluetoothGattService
    private val maTxCharacteristic: BluetoothGattCharacteristic
        get() = meshAccessService.getCharacteristic(MA_UUID_TX_CHAR)
    private val maRxCharacteristic: BluetoothGattCharacteristic
        get() = meshAccessService.getCharacteristic(MA_UUID_RX_CHAR)

    val encryptionState: MutableLiveData<EncryptionState> = MutableLiveData()

    override fun getGattCallback(): BleManagerGattCallback {
        return MataGeekBleManagerGattCallback()
    }

    private val meshAccessDataCallback: MeshAccessDataCallback =
        object : MeshAccessDataCallback() {
            override fun onDataSent(device: BluetoothDevice, data: Data) {
                Log.d("MATAG", "onDataSent: ")
            }
        }

    fun startEncryptionHandshake() {
        encryptionState.postValue(EncryptionState.ENCRYPTING)
        val connPacketEncryptCustomStart =
            ConnPacketEncryptCustomStart(nodeId, 0, 1, FmKeyId.NETWORK, 1, 0)
        writeCharacteristic(maRxCharacteristic,
            Data(connPacketEncryptCustomStart.createPacket()))
            .split(FruityDataSplitter(null, null)).enqueue()
    }

    private inner class MataGeekBleManagerGattCallback : BleManagerGattCallback() {
        override fun initialize() {
            setNotificationCallback(maTxCharacteristic).with(meshAccessDataCallback)
            enableNotifications(maTxCharacteristic).enqueue()
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val maService: BluetoothGattService? = gatt.getService(MA_UUID_SERVICE)
            val maRxCharacteristic = maService?.getCharacteristic(MA_UUID_RX_CHAR)
            val maTxCharacteristic = maService?.getCharacteristic(MA_UUID_TX_CHAR)
            if (maRxCharacteristic == null || maTxCharacteristic == null) return false
            meshAccessService = maService

            return (maRxCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0
        }

        override fun onDeviceDisconnected() {
            Log.d("MATAG", "onDeviceDisconnected: ")
        }

    }

    companion object {
        /** MeshAccessService UUID */
        val MA_UUID_SERVICE: UUID = UUID.fromString("00000001-acce-423c-93fd-0c07a0051858")

        /** RX characteristic UUID (use for send packet to peripheral) */
        val MA_UUID_RX_CHAR: UUID = UUID.fromString("00000002-acce-423c-93fd-0c07a0051858")

        /** TX characteristic UUID (use for receive packet from peripheral) */
        val MA_UUID_TX_CHAR: UUID = UUID.fromString("00000003-acce-423c-93fd-0c07a0051858")

    }

}