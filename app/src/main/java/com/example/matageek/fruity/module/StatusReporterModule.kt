package com.example.matageek.fruity.module

import android.util.Log
import com.example.matageek.customexception.MessagePacketSizeException
import com.example.matageek.fruity.types.ConnPacketModule
import com.example.matageek.fruity.types.MessageType
import com.example.matageek.fruity.types.ModuleId
import com.example.matageek.fruity.types.ModuleIdWrapper
import com.example.matageek.manager.DeviceInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.Exception

class StatusReporterModule : Module("status", ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id)) {

    override fun actionResponseMessageReceivedHandler(packet: ByteArray) {
        val modulePacket = ConnPacketModule(packet)
        when (modulePacket.actionType) {
            StatusModuleActionResponseMessages.STATUS.type -> {
                val statusMessage =
                    StatusReporterModuleStatusMessage.readFromBytePacket(packet.copyOfRange(
                        ConnPacketModule.SIZEOF_PACKET, packet.size))
                Log.d("MATAG", "actionResponseMessageReceivedHandler: $statusMessage")
            }
            StatusModuleActionResponseMessages.ALL_CONNECTIONS.type -> {
                val allConnectionsMessage = StatusReporterModuleConnectionsMessage(
                    packet.copyOfRange(ConnPacketModule.SIZEOF_PACKET, packet.size))
                Log.d("MATAG", "ID:${modulePacket.header.sender}\n" +
                        "partner1: ${allConnectionsMessage.partner1}\n" +
                        "partner2: ${allConnectionsMessage.partner2}\n" +
                        "partner3: ${allConnectionsMessage.partner3}\n" +
                        "partner4: ${allConnectionsMessage.partner4}\n")
            }
        }
    }

    class StatusReporterModuleStatusMessage(
        val clusterSize: Short, val inConnectionPartner: Short, val inConnectionRSSI: Byte,
        val freeInAndOut: Byte, val batteryInfo: Byte, val connectionLossCounter: Byte,
        val initializedByGateway: Byte,
    ) {
        companion object {
            private const val SIZEOF_PACKET = 9
            fun readFromBytePacket(bytePacket: ByteArray): StatusReporterModuleStatusMessage {
                if (bytePacket.size < SIZEOF_PACKET) throw MessagePacketSizeException(this::class.java.toString(),
                    SIZEOF_PACKET)
                val byteBuffer = ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN)
                return StatusReporterModuleStatusMessage(byteBuffer.short,
                    byteBuffer.short, byteBuffer.get(), byteBuffer.get(), byteBuffer.get(),
                    byteBuffer.get(), byteBuffer.get())
            }
        }
    }

    class StatusReporterModuleConnectionsMessage(packet: ByteArray) {
        val partner1: Short
        val rssi1: Byte
        val partner2: Short
        val rssi2: Byte
        val partner3: Short
        val rssi3: Byte
        val partner4: Short
        val rssi4: Byte

        companion object {
            const val SIZE_OF_PACKET = 12
        }

        init {
            if (packet.size < SIZE_OF_PACKET) throw Exception("invalid packet")
            val byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
            partner1 = byteBuffer.short
            rssi1 = byteBuffer.get()
            partner2 = byteBuffer.short
            rssi2 = byteBuffer.get()
            partner3 = byteBuffer.short
            rssi3 = byteBuffer.get()
            partner4 = byteBuffer.short
            rssi4 = byteBuffer.get()
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
    }

}