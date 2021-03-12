package com.example.matageek.fruity.module

import android.util.Log
import com.example.matageek.fruity.types.*
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EnrollmentModule : Module("enroll", FmTypes.ModuleId.ENROLLMENT_MODULE.id) {

    fun createEnrollmentBroadcastAppStartMessagePacket(receiver: Short, key: ByteArray): ByteArray {
        val data = EnrollmentModuleSetEnrollmentBroadcastAppStartMessage(
            1, 11, key, key, key, key, 10).createBytePacket()
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

    enum class EnrollmentResponseCode(val code: Byte) {
        OK(0x00),

        // There are more enroll response codes that are taken from the Flash Storage response codes
        ALREADY_ENROLLED_WITH_DIFFERENT_DATA(0x10),
        PREENROLLMENT_FAILED(0x11),
        SIG_CONFIGURATION_INVALID(0x12),
        INCORRECT_NODE_ID(0x13),
        HIGHEST_POSSIBLE_VALUE(0xFF.toByte())
    }

    class EnrollmentModuleEnrollmentResponse(packet: ByteArray) {
        val serialNumberIndex: Int
        val enrollmentResponseCode: Byte

        companion object {
            const val SIZE_OF_PACKET = 5
        }

        init {
            if (packet.size < SIZE_OF_PACKET) throw Exception("invalid packet")
            val byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
            serialNumberIndex = byteBuffer.int
            enrollmentResponseCode = byteBuffer.get()
        }
    }

    override fun actionResponseMessageReceivedHandler(packet: ByteArray) {
        Log.d("MATAG", "actionResponseMessageReceivedHandler: ${Data(packet)}")
        if (packet.size < ConnPacketModule.SIZEOF_PACKET + EnrollmentModuleEnrollmentResponse.SIZE_OF_PACKET)
            throw Exception("invalid packet")

        val connModule = ConnPacketModule.readFromBytePacket(packet.copyOfRange(0,
            ConnPacketModule.SIZEOF_PACKET)) ?: throw Exception("invalid packet")

        val response = packet.copyOfRange(ConnPacketModule.SIZEOF_PACKET,
            ConnPacketModule.SIZEOF_PACKET + EnrollmentModuleEnrollmentResponse.SIZE_OF_PACKET)
        val enrollResponse = EnrollmentModuleEnrollmentResponse(response)

        when (connModule.actionType) {
            EnrollmentModuleActionResponseMessages.ENROLLMENT_RESPONSE.type -> {
                when (enrollResponse.enrollmentResponseCode) {
                    EnrollmentResponseCode.PREENROLLMENT_FAILED.code -> {
                        Log.d("MATAG", "NodeId:${connModule.header.sender} enroll failed")
                    }
                    EnrollmentResponseCode.OK.code -> {
                        Log.d("MATAG", "NodeId:${connModule.header.sender} enroll success")
                    }
                }
            }
        }

    }
}