package com.example.matageek.fruity.module

import com.example.matageek.customexception.MessagePacketSizeException
import com.example.matageek.fruity.types.ConnPacketVendorModule
import com.example.matageek.fruity.types.MessageType
import com.example.matageek.fruity.types.PrimitiveTypes
import com.example.matageek.manager.DeviceInfo

class MatageekModule : Module("matageek", PrimitiveTypes.getVendorModuleId(
    0xAB24.toShort(), 1)
) {
    override fun actionResponseMessageReceivedHandler(packet: ByteArray) {
        val vendorModulePacket = ConnPacketVendorModule(packet)
        when (vendorModulePacket.actionType) {
            MatageekModuleActionResponseMessages.TRAP_STATE_RESPONSE.type -> {
                val trapStateMessage = MatageekModuleTrapStateMessage(packet.copyOfRange(
                    ConnPacketVendorModule.SIZEOF_PACKET, packet.size))
                notifyObserver(DeviceInfo(null, null, trapStateMessage.trapState, null))
            }
        }
    }

    fun createTrapStateMessagePacket(
        receiver: Short, actionType: MatageekModuleTriggerActionMessages,
    ): ByteArray {
        return createSendModuleActionMessagePacket(MessageType.MODULE_TRIGGER_ACTION,
            receiver, 0, actionType.type, null, 0, false)
    }

    class MatageekModuleTrapStateMessage(packet: ByteArray) {
        val trapState: Boolean

        init {
            if (packet.size != SIZEOF_PACKET) throw MessagePacketSizeException(
                this::class.java.toString(), SIZEOF_PACKET)
            trapState = packet[0] == 1.toByte()
        }

        companion object {
            const val SIZEOF_PACKET = 1
        }
    }

    enum class MatageekModuleTriggerActionMessages(
        val type: Byte,
    ) {
        TRAP_STATE(0),
        TRAP_FIRE(1),
        MODE_CHANGE(2),
        BATTERY_DEAD(3),
    }

    enum class MatageekModuleActionResponseMessages(val type: Byte) {
        TRAP_STATE_RESPONSE(0),
    }

}