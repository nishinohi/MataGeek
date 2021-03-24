package com.example.matageek.manager

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.matageek.R
import com.example.matageek.fruity.module.EnrollmentModule
import com.example.matageek.fruity.module.MatageekModule
import com.example.matageek.fruity.module.Module
import com.example.matageek.fruity.module.StatusReporterModule
import com.example.matageek.fruity.types.*
import com.example.matageek.profile.callback.MeshAccessDataCallback
import com.example.matageek.profile.callback.EncryptionState
import com.example.matageek.profile.FruityDataEncryptAndSplit
import no.nordicsemi.android.ble.callback.SuccessCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.Exception

class MeshAccessManager(context: Context) :
    ObservableBleManager(context), MeshAccessObserver {

    /** MeshAccessService Characteristics */
    private lateinit var meshAccessService: BluetoothGattService
    val handShakeState: MutableLiveData<HandShakeState> = MutableLiveData()
    private val modules: MutableList<Module> = mutableListOf()

    // Node info
    val clusterSize: MutableLiveData<Short> = MutableLiveData()
    val batteryInfo: MutableLiveData<Byte> = MutableLiveData()
    val trapState: MutableLiveData<Boolean> = MutableLiveData()
    val modeState: MutableLiveData<MatageekModule.MatageekMode> = MutableLiveData()

    // ble timeout handler map
    val timeoutMap: MutableMap<Long, TimeOutCoroutineJobAndCounter<Boolean>> = mutableMapOf()

    data class TimeOutCoroutineJobAndCounter<T>(
        var counter: Short,
        val successCallback: () -> Unit,
    )

    fun addTimeoutJob(
        moduleId: Int,
        actionType: Byte,
        requestHandle: Byte,
        counter: Short,
        successCallback: () -> Unit,
    ) {
        timeoutMap[generateTimeoutKey(moduleId, actionType, requestHandle)] =
            TimeOutCoroutineJobAndCounter(counter, successCallback)
    }

    fun generateTimeoutKey(moduleId: Int, actionType: Byte, requestHandle: Byte): Long {
        val byteBuffer = ByteBuffer.allocate(Long.SIZE_BYTES).putInt(moduleId)
            .put(actionType).put(requestHandle)
        byteBuffer.clear()
        return byteBuffer.long
    }

    // TODO not secure
    private val defaultKeyInt = 0x22222222

    // TODO not secure
    private val networkKeyPreference: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.preference_network_key),
            Context.MODE_PRIVATE)

    init {
        modules.add(StatusReporterModule())
        modules.add(EnrollmentModule())
        modules.add(MatageekModule())
        modules.forEach { it.addObserver(this) }
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return MataGeekBleManagerGattCallback()
    }

    private val meshAccessDataCallback: MeshAccessDataCallback =
        object : MeshAccessDataCallback() {
            private val messageBuffer = mutableListOf<Byte>()

            override fun sendPacket(
                data: ByteArray, encryptionNonce: Array<Int>?, encryptionKey: SecretKey?,
                callback: SuccessCallback?,
            ) {
                val request = writeCharacteristic(this.maRxCharacteristic, Data(data))
                    .split(FruityDataEncryptAndSplit(encryptionNonce, encryptionKey)).with(this)
                if (callback != null) request.done(callback)
                request.enqueue()
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
                            ConnPacketEncryptCustomANonce(packet)
                        onANonceReceived(connPacketEncryptCustomANonce)
                    }
                    MessageType.ENCRYPT_CUSTOM_DONE -> {
                        this@MeshAccessManager.handShakeState.postValue(HandShakeState.HANDSHAKE_DONE)
                        val callback = SuccessCallback {
                            sendModuleActionTriggerMessage(
                                PrimitiveTypes.getVendorModuleId(VendorModuleId.MATAGEEK_MODULE.id,
                                    1),
                                MatageekModule.MatageekModuleTriggerActionMessages.STATE.type
                            )
                        }
                        sendModuleActionTriggerMessage(
                            ModuleId.STATUS_REPORTER_MODULE.id.toUByte().toInt(),
                            StatusReporterModule.StatusModuleTriggerActionMessages.GET_STATUS.type,
                            this.partnerId, null, 0, callback)
//                        sendStatusTriggerActionMessage(StatusReporterModule.StatusModuleTriggerActionMessages.GET_ALL_CONNECTIONS,
//                            PrimitiveTypes.NODE_ID_BROADCAST)
                    }
                    MessageType.SPLIT_WRITE_CMD -> {
                        val splitPacket = PacketSplitHeader(packet)
                        if (splitPacket.splitCounter == 0.toByte()) messageBuffer.clear()
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
                            ConnPacketClusterInfoUpdate(packet)
                        update(DeviceInfo(clusterInfoUpdate.clusterSizeChange, null))
                    }
                    else -> {
                        Log.d("MATAG", "onDataReceived: Unknown Message $messageType")
                    }
                }
            }
        }

    fun moduleMessageReceivedHandler(packet: ByteArray) {
        val modulePacket = ConnPacketModule(packet)
        modules.find { it.moduleId != 0.toByte() && modulePacket.moduleId == it.moduleId }
            ?.actionResponseMessageReceivedHandler(packet)
        val vendorModulePacket = ConnPacketVendorModule(packet)
        modules.find { it.vendorModuleId != 0 && vendorModulePacket.vendorModuleId == it.vendorModuleId }
            ?.actionResponseMessageReceivedHandler(packet)

        val isVendorModuleId = PrimitiveTypes.isVendorModuleId(modulePacket.moduleId)
        val wrapperModuleId = if (isVendorModuleId) vendorModulePacket.vendorModuleId else
            ModuleIdWrapper(modulePacket.moduleId).wrappedModuleId
        val actionType =
            if (isVendorModuleId) vendorModulePacket.actionType else modulePacket.actionType
        val requestHandle =
            if (isVendorModuleId) vendorModulePacket.requestHandle else modulePacket.requestHandle
        val timeoutKey = generateTimeoutKey(wrapperModuleId, actionType, requestHandle)
        timeoutMap[timeoutKey]?.let {
            --(it.counter)
            Log.d("MATAG", "timeout counter: ${it.counter}")
            if (it.counter == 0.toShort()) {
                Log.d("MATAG", "timeout counter: job cancel")
                it.successCallback()
                timeoutMap.remove(timeoutKey)
            }
        }
    }

    override fun update(deviceInfo: DeviceInfo) {
        deviceInfo.clusterSize?.let { this.clusterSize.postValue(it) }
        deviceInfo.batteryInfo?.let { this.batteryInfo.postValue(it) }
        deviceInfo.trapState?.let { this.trapState.postValue(it) }
        deviceInfo.matageekMode?.let { this.modeState.postValue(it) }
    }

    private fun <T : Module> findModuleById(moduleId: Int): T {
        val module = modules.find {
            (it.vendorModuleId == 0 && it.moduleId == moduleId.toByte()) ||
                    (it.moduleId == 0.toByte() && it.vendorModuleId == moduleId)
        }
            ?: throw Exception("Module not found")
        return module as? T
            ?: throw Exception("${module::class.java.toString()} is not cast type")
    }

    // TODO send module message by another MODEL class

    fun startEncryptionHandshake(isEnrolled: Boolean) {
        handShakeState.postValue(HandShakeState.HANDSHAKING)
        // TODO not secure
        if (isEnrolled) {
            val key =
                networkKeyPreference.getInt(context.getString(R.string.network_key),
                    defaultKeyInt)
            val byteBuffer =
                ByteBuffer.allocate(16).putInt(key).putInt(key).putInt(key).putInt(key)
            Log.d("MATAG", "startEncryptionHandshake: ${Data(byteBuffer.array())}")
            meshAccessDataCallback.networkKey = SecretKeySpec(byteBuffer.array(), "AES")
        }
        meshAccessDataCallback.startEncryptionHandshake()
    }

    fun sendEnrollmentBroadcastAppStart() {
        val enrollModule =
            modules.find { it.moduleId == ModuleId.ENROLLMENT_MODULE.id }
                ?: throw Exception("Module not exist")

        // TODO not secure!!
        if (!networkKeyPreference.contains(context.getString(R.string.network_key))) {
            networkKeyPreference.edit().apply {
                putInt(context.getString(R.string.network_key),
                    SecureRandom.getInstance("SHA1PRNG").nextInt())
                apply()
            }
        }
        val key =
            networkKeyPreference.getInt(context.getString(R.string.network_key),
                defaultKeyInt)
        val byteBuffer = ByteBuffer.allocate(16).putInt(key).putInt(key).putInt(key).putInt(key)

        meshAccessDataCallback.sendPacket(
            (enrollModule as EnrollmentModule).createEnrollmentBroadcastAppStartMessagePacket(
                meshAccessDataCallback.partnerId, byteBuffer.array()),
            meshAccessDataCallback.encryptionNonce,
            meshAccessDataCallback.encryptionKey)
    }

    fun sendModuleActionTriggerMessage(
        moduleId: Int, actionType: Byte, receiver: Short = meshAccessDataCallback.partnerId,
        additionalData: ByteArray? = null, additionalDataSize: Int = 0,
        callback: SuccessCallback? = null,
    ) {
        val module = findModuleById<Module>(moduleId)
        meshAccessDataCallback.sendPacket(
            module.createTriggerActionMessagePacket(receiver,
                actionType, additionalData, additionalDataSize),
            meshAccessDataCallback.encryptionNonce,
            meshAccessDataCallback.encryptionKey, callback)
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

    enum class HandShakeState(val state: Byte) {
        HANDSHAKE_NONE(0),
        HANDSHAKING(1),
        HANDSHAKE_DONE(2)
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