package com.example.matageek.fruity.module

import com.example.matageek.fruity.types.ConnPacketHeader
import com.example.matageek.fruity.types.ConnPacketModule
import com.example.matageek.fruity.types.FmTypes
import com.example.matageek.fruity.types.MessageType
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StatusReporterModule() : Module("status", FmTypes.ModuleId.STATUS_REPORTER_MODULE.id) {

    fun createGetStatusMessagePacket(receiver: Short): ByteArray {
        return createSendModuleActionMessagePacket(MessageType.MODULE_TRIGGER_ACTION, receiver, 0,
            StatusModuleTriggerActionMessages.GET_STATUS.type, null, 0, false)
    }

    class StatusReporterModuleStatusMessage(
        val clusterSize: Short, val inConnectionPartner: Short, val inConnectionRSSI: Byte,
        val freeInAndOut: Byte, val batteryInfo: Byte, val connectionLossCounter: Byte,
        val initializedByGateway: Byte,
    ) {
        companion object {
            const val SIZEOF_PACKET = 9
            fun readFromBytePacket(bytePacket: ByteArray): StatusReporterModuleStatusMessage {
                val byteBuffer = ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN)
                return StatusReporterModuleStatusMessage(byteBuffer.short,
                    byteBuffer.short, byteBuffer.get(), byteBuffer.get(), byteBuffer.get(),
                    byteBuffer.get(), byteBuffer.get())
            }
        }
    }

    enum class StatusModuleTriggerActionMessages(val type: Byte) {
        SET_LED(0),
        GET_STATUS(1),

        //GET_DEVICE_INFO(2), removed as of 17.05.2019
        GET_ALL_CONNECTIONS(3),
        GET_NEARBY_NODES(4),
        SET_INITIALIZED(5),
        GET_ERRORS(6),
        GET_REBOOT_REASON(8),
        SET_KEEP_ALIVE(9),
        GET_DEVICE_INFO_V2(10),
        SET_LIVEREPORTING(11),
        GET_ALL_CONNECTIONS_VERBOSE(12),
    };

    enum class StatusModuleActionResponseMessages(val type: Byte) {
        SET_LED_RESULT(0),
        STATUS(1),

        //DEVICE_INFO(2), removed as of 17.05.2019
        ALL_CONNECTIONS(3),
        NEARBY_NODES(4),
        SET_INITIALIZED_RESULT(5),
        ERROR_LOG_ENTRY(6),

        //DISCONNECT_REASON(7), removed as of 21.05.2019
        REBOOT_REASON(8),
        DEVICE_INFO_V2(10),
        ALL_CONNECTIONS_VERBOSE(12),
    };

}