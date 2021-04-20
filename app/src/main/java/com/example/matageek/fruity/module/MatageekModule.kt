package com.example.matageek.fruity.module

import com.example.matageek.customexception.MessagePacketSizeException
import com.example.matageek.fruity.types.*
import com.example.matageek.manager.DeviceInfo

class MatageekModule : Module("matageek", PrimitiveTypes.getVendorModuleId(
    0xAB24.toShort(), 1)
) {
    override fun actionResponseMessageReceivedHandler(packet: ByteArray) {
        val vendorModulePacket = ConnPacketVendorModule(packet)
        when (vendorModulePacket.actionType) {
            MatageekModuleActionResponseMessages.STATE_RESPONSE.type -> {
                val trapStateMessage = MatageekModuleStateMessage(packet.copyOfRange(
                    ConnPacketVendorModule.SIZEOF_PACKET, packet.size))
                notifyObserver(DeviceInfo(vendorModulePacket.header.sender,
                    null, null, trapStateMessage.trapState,
                    null, MatageekMode.getMode(trapStateMessage.mode)))
            }
            MatageekModuleActionResponseMessages.MODE_CHANGE_RESPONSE.type -> {
                val responseCode = MatageekModuleResponse(packet.copyOfRange(
                    ConnPacketVendorModule.SIZEOF_PACKET, packet.size))
            }
        }
    }

    class MatageekModuleStateMessage : ConnectionMessageTypes {
        val trapState: Boolean
        val mode: Byte

        constructor(packet: ByteArray) {
            if (packet.size != SIZEOF_PACKET) throw MessagePacketSizeException(
                this::class.java.toString(), SIZEOF_PACKET)
            val byteBuffer = getByteBufferWrap(packet)
            trapState = byteBuffer.get() == 1.toByte()
            mode = byteBuffer.get()
        }

        companion object {
            const val SIZEOF_PACKET = 2
        }

        override fun createBytePacket(): ByteArray {
            TODO("Not yet implemented")
        }
    }

    class MatageekModuleModeChangeMessage : ConnectionMessageTypes {
        val matageekMode: MatageekMode
        val clusterSize: Short

        constructor(mode: MatageekMode, clusterSize: Short) {
            this.matageekMode = mode
            this.clusterSize = clusterSize
        }

        constructor(packet: ByteArray) {
            if (packet.size < SIZEOF_PACKET) throw MessagePacketSizeException(
                this::class.java.toString(), SIZEOF_PACKET)
            val byteBuffer = getByteBufferWrap(packet)
            matageekMode = MatageekMode.getMode(byteBuffer.get())
            clusterSize = byteBuffer.short
        }

        companion object {
            const val SIZEOF_PACKET = 3
        }

        override fun createBytePacket(): ByteArray {
            val byteBuffer = getByteBufferAllocate(SIZEOF_PACKET)
            byteBuffer.put(matageekMode.mode)
            byteBuffer.putShort(clusterSize)
            return byteBuffer.array()
        }
    }

    class MatageekModuleResponse : ConnectionMessageTypes {
        val responseCode: MatageekResponseCode

        constructor(packet: ByteArray) {
            val byteBuffer = getByteBufferAllocate(SIZEOF_PACKET)
            responseCode = getResponseCode(byteBuffer.get())
        }

        override fun createBytePacket(): ByteArray {
            TODO("Not yet implemented")
        }

        companion object {
            fun getResponseCode(code: Byte): MatageekResponseCode {
                return MatageekResponseCode.values().find { it.code == code }
                    ?: MatageekResponseCode.FAILED
            }

            const val SIZEOF_PACKET = 1
        }
    }

    enum class MatageekResponseCode(val code: Byte) {
        OK(0),
        FAILED(1),
    }

    enum class MatageekModuleTriggerActionMessages(
        val type: Byte,
    ) {
        STATE(0),
        TRAP_FIRE(1),
        MODE_CHANGE(2),
        BATTERY_DEAD(3),
    }

    enum class MatageekModuleActionResponseMessages(val type: Byte) {
        STATE_RESPONSE(0),
        TRAP_FIRE_RESPONSE(1),
        MODE_CHANGE_RESPONSE(2),
        BATTERY_DEAD_RESPONSE(3),
    }

    enum class MatageekMode(val mode: Byte) {
        SETUP(0),
        DETECT(1);

        companion object {
            fun getMode(mode: Byte): MatageekMode {
                return MatageekMode.values().find { it.mode == mode }
                    ?: throw IllegalArgumentException(this::class.java.toString())
            }
        }

    }

}