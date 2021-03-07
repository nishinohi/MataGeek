package com.example.matageek.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.matageek.fruity.module.Module
import com.example.matageek.fruity.module.StatusReporterModule
import com.example.matageek.fruity.types.*
import com.example.matageek.profile.callback.MeshAccessDataCallback
import com.example.matageek.profile.callback.EncryptionState
import com.example.matageek.profile.FruityDataEncryptAndSplit
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import java.util.*
import javax.crypto.SecretKey
import kotlin.Exception

class MeshAccessManager(context: Context) :
    ObservableBleManager(context), MeshAccessObserver {

    /** MeshAccessService Characteristics */
    private lateinit var meshAccessService: BluetoothGattService
    private val modules: MutableList<Module> = mutableListOf()
    val clusterSize: MutableLiveData<Short> = MutableLiveData()
    val batteryInfo: MutableLiveData<Byte> = MutableLiveData()

    init {
        modules.add(StatusReporterModule())
        modules.forEach { it.addObserver(this) }
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return MataGeekBleManagerGattCallback()
    }

    private val meshAccessDataCallback: MeshAccessDataCallback =
        object : MeshAccessDataCallback() {
            private val messageBuffer = mutableListOf<Byte>()

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

            override fun parsePacket(packet: ByteArray) {
                when (val messageType = MessageType.getMessageType(packet[0])) {
                    MessageType.ENCRYPT_CUSTOM_ANONCE -> {
                        if (encryptionState.value != EncryptionState.ENCRYPTING) return
                        val connPacketEncryptCustomANonce =
                            ConnPacketEncryptCustomANonce.readFromBytePacket(packet)
                                ?: throw Exception("invalid message")
                        onANonceReceived(connPacketEncryptCustomANonce)
                    }
                    MessageType.ENCRYPT_CUSTOM_DONE -> {
                        sendGetStatusMessage()
                    }
                    MessageType.SPLIT_WRITE_CMD -> {
                        val splitPacket = PacketSplitHeader.readFromBytePacket(packet)
                            ?: throw Exception("invalid message")
                        if (splitPacket.splitCounter.toInt() == 0) messageBuffer.clear()
                        messageBuffer.addAll(packet.copyOfRange(PacketSplitHeader.SIZEOF_PACKET,
                            packet.size).toList())
                    }
                    MessageType.SPLIT_WRITE_CMD_END -> {
                        messageBuffer.addAll(packet.copyOfRange(PacketSplitHeader.SIZEOF_PACKET,
                            packet.size).toList())
                        parsePacket(messageBuffer.toByteArray())
                    }
                    MessageType.MODULE_ACTION_RESPONSE -> {
                        moduleMessageReceivedHandler(packet)
                    }
                    MessageType.CLUSTER_INFO_UPDATE -> {
                        val clusterInfoUpdate =
                            ConnPacketClusterInfoUpdate.readFromBytePacket(packet)
                                ?: throw Exception("invalid message")
                        update(DeviceInfo(clusterInfoUpdate.clusterSizeChange, null))
                    }
                    else -> {
                        Log.d("MATAG", "onDataReceived: Unknown Message $messageType")
                    }
                }
            }
        }

    fun moduleMessageReceivedHandler(packet: ByteArray) {
        val modulePacketHeader = ConnPacketModule.readFromBytePacket(packet)
            ?: throw Exception("mesh Message Error")
        modules.find { modulePacketHeader.moduleId == it.moduleId }
            ?.actionResponseMessageReceivedHandler(packet)
    }

    override fun update(deviceInfo: DeviceInfo) {
        deviceInfo.clusterSize?.let { this.clusterSize.postValue(it) }
        deviceInfo.batteryInfo?.let { this.batteryInfo.postValue(it) }
    }

    fun startEncryptionHandshake() {
        meshAccessDataCallback.startEncryptionHandshake()
    }

    fun sendGetStatusMessage() {
        val statusReporterModule =
            modules.find { it.moduleId == FmTypes.ModuleId.STATUS_REPORTER_MODULE.id }
                ?: throw Exception("Module not exist")

        writeCharacteristic(this.meshAccessDataCallback.maRxCharacteristic,
            (statusReporterModule as StatusReporterModule).createGetStatusMessagePacket(
                meshAccessDataCallback.partnerId))
            .split(FruityDataEncryptAndSplit(this.meshAccessDataCallback.encryptionNonce,
                this.meshAccessDataCallback.encryptionKey)).with(this.meshAccessDataCallback)
            .enqueue()
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
        val MESH_SERVICE_DATA_SERVICE_UUID16: UUID =
            UUID.fromString("0000FE12-0000-0000-0000-000000000000")
        val MA_UUID_SERVICE: UUID = UUID.fromString("00000001-acce-423c-93fd-0c07a0051858")

        /** RX characteristic UUID (use for send packet to peripheral) */
        val MA_UUID_RX_CHAR: UUID = UUID.fromString("00000002-acce-423c-93fd-0c07a0051858")

        /** TX characteristic UUID (use for receive packet from peripheral) */
        val MA_UUID_TX_CHAR: UUID = UUID.fromString("00000003-acce-423c-93fd-0c07a0051858")

    }

}