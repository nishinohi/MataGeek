package com.example.matageek.fruity.module

import android.util.Log
import com.example.matageek.fruity.types.ConnPacketHeader
import com.example.matageek.fruity.types.ConnectionMessageTypes
import com.example.matageek.fruity.types.FmTypes
import com.example.matageek.fruity.types.MessageType
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EnrollmentModule : Module("enroll", FmTypes.ModuleId.ENROLLMENT_MODULE.id) {

    fun createEnrollmentBroadcastAppStartMessagePacket(receiver: Short): ByteArray {
        val data = EnrollmentModuleSetEnrollmentBroadcastAppStartMessage(
            1, 11,
            byteArrayOf(12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12),
            byteArrayOf(12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12),
            byteArrayOf(12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12),
            byteArrayOf(12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12),
            10).createBytePacket()
        return createSendModuleActionMessagePacket(
            MessageType.MODULE_TRIGGER_ACTION, receiver,
            0, EnrollmentModuleActionResponseMessages.SET_ENROLLMENT_BROADCAST_APP_START.type,
            data, data.size, false)
    }

    class EnrollmentModuleSetEnrollmentBroadcastAppStartMessage(
        val newNodeIdOffset: Short,
        val newNetworkId: Short,
        val newNetworkKey: ByteArray,
        val newUserBaseKey: ByteArray,
        val newOrganizationKey: ByteArray,
        val nodeKey: ByteArray, // Key used to connect to the unenrolled node
        val timeoutSec: Byte, //how long to try to connect to the unenrolled node, 0 means default time
    ) : ConnectionMessageTypes {
        val clusterSize: Short = 0

        companion object {
            const val SIZEOF_PACKET = 71
            const val SIZEOF_PACKET_MIN = 6
            fun readFromBytePacket(bytePacket: ByteArray): EnrollmentModuleSetEnrollmentBroadcastAppStartMessage? {
                if (bytePacket.size < SIZEOF_PACKET) return null
                val byteBuffer = ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN)
                val newNodeIdOffset = byteBuffer.short
                val newNetworkId = byteBuffer.short
                val clusterSize = byteBuffer.short
                val newNetworkKey: ByteArray = ByteArray(FmTypes.SECRET_KEY_LENGTH)
                byteBuffer.get(newNetworkKey, 0, FmTypes.SECRET_KEY_LENGTH)
                val newUserBaseKey: ByteArray = ByteArray(FmTypes.SECRET_KEY_LENGTH)
                byteBuffer.get(newUserBaseKey, 0, FmTypes.SECRET_KEY_LENGTH)
                val newOrganizationKey: ByteArray = ByteArray(FmTypes.SECRET_KEY_LENGTH)
                byteBuffer.get(newOrganizationKey, 0, FmTypes.SECRET_KEY_LENGTH)
                val nodeKey: ByteArray = ByteArray(FmTypes.SECRET_KEY_LENGTH)
                byteBuffer.get(nodeKey, 0, FmTypes.SECRET_KEY_LENGTH)
                return EnrollmentModuleSetEnrollmentBroadcastAppStartMessage(
                    newNodeIdOffset, newNetworkId, newNetworkKey, newUserBaseKey,
                    newOrganizationKey, nodeKey, byteBuffer.get())
            }
        }

        override fun createBytePacket(): ByteArray {
            val byteBuffer: ByteBuffer =
                ByteBuffer.allocate(SIZEOF_PACKET).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.putShort(newNodeIdOffset).putShort(newNetworkId).putShort(clusterSize)
                .put(newNetworkKey).put(newUserBaseKey).put(newOrganizationKey).put(nodeKey)
                .put(timeoutSec)
            return byteBuffer.array()
        }
    }

    enum class EnrollmentModuleActionResponseMessages(val type: Byte) {
        ENROLLMENT_RESPONSE(0),
        REMOVE_ENROLLMENT_RESPONSE(1),
        ENROLLMENT_PROPOSAL(2),
        SET_NETWORK_RESPONSE(3),
        REQUEST_PROPOSALS_RESPONSE(4),
        SET_ENROLLMENT_BROADCAST(5),
        ENROLLMENT_BROADCAST_SERIAL(6),
        SET_ENROLLMENT_BROADCAST_APP_START(7),

    }

    enum class EnrollmentModuleTriggerActionMessages(val type: Byte) {
        SET_ENROLLMENT_BY_SERIAL(0),
        REMOVE_ENROLLMENT(1),

        //SET_ENROLLMENT_BY_SERIAL = 2, //Deprecated since version 0.7.22
        SET_NETWORK(3),
        REQUEST_PROPOSALS(4),
        ENROLLMENT_BROADCAST_RESPONSE(5),
        ENROLLMENT_BROADCAST_SERIAL_RESPONSE(6),
        SET_ENROLLMENT_BROADCAST_APP_START_RESPONSE(7),
    }

    override fun actionResponseMessageReceivedHandler(packet: ByteArray) {
        Log.d("MATAG", "actionResponseMessageReceivedHandler: ${Data(packet)}")
    }
}