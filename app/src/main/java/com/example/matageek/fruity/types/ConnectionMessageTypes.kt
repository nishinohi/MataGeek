package com.example.matageek.fruity.types

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface ConnectionMessageTypes {
    fun createBytePacket(): ByteArray

    companion object {
        const val SIS = 0
    }
}

class ConnPacketHeader(
    val messageType: MessageType,
    val sender: Short,
    val receiver: Short,
) : ConnectionMessageTypes {

    companion object {
        const val SIZEOF_PACKET = 5
        fun readFromBytePacket(bytePacket: ByteArray): ConnPacketHeader? {
            if (bytePacket.size < SIZEOF_PACKET) return null
            val byteBuffer = ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN)
            return ConnPacketHeader(
                MessageType.getMessageType(byteBuffer.get()), byteBuffer.short, byteBuffer.short)
        }
    }

    override fun createBytePacket(): ByteArray {
        val byteBuffer: ByteBuffer =
            ByteBuffer.allocate(SIZEOF_PACKET).order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(messageType.typeValue)
        byteBuffer.putShort(sender)
        byteBuffer.putShort(receiver)
        return byteBuffer.array()
    }

}

class ConnPacketModule(
    messageType: MessageType,
    sender: Short,
    receiver: Short,
    val moduleId: Byte,
    val requestHandle: Byte,
    val actionType: Byte,
) {
    val header: ConnPacketHeader = ConnPacketHeader(messageType, sender, receiver)

    companion object {
        var SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 3
        fun readFromBytePacket(bytePacket: ByteArray): ConnPacketModule? {
            if (bytePacket.size < SIZEOF_PACKET) return null
            val byteBuffer = ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN)
            return ConnPacketModule(
                MessageType.getMessageType(byteBuffer.get()), byteBuffer.short, byteBuffer.short,
                byteBuffer.get(), byteBuffer.get(), byteBuffer.get()
            )
        }
    }
}

class PacketSplitHeader(
    val splitMessageType: MessageType,
    val splitCounter: Byte,
) {
    companion object {
        var SIZEOF_PACKET = 2
        fun readFromBytePacket(bytePacket: ByteArray): PacketSplitHeader? {
            if (bytePacket.size < SIZEOF_PACKET) return null
            val byteBuffer = ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN)
            return PacketSplitHeader(
                MessageType.getMessageType(byteBuffer.get()), byteBuffer.get())
        }
    }
}

class ConnPacketEncryptCustomStart(
    sender: Short,
    receiver: Short,
    val version: Byte,
    private val fmKeyId: FmKeyId,
    private val tunnelType: Byte,
    val reserved: Byte,
) : ConnectionMessageTypes {
    private val header: ConnPacketHeader =
        ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_START, sender, receiver)

    companion object {
        const val SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 6
    }

    override fun createBytePacket(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(SIZEOF_PACKET).order(
            ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(header.createBytePacket())
        byteBuffer.put(version)
        byteBuffer.putInt(fmKeyId.keyId)
        val concatByte: Byte = (((reserved.toInt()) shl 2) or tunnelType.toInt()).toByte()
        byteBuffer.put(concatByte)
        return byteBuffer.array()
    }

}

class ConnPacketEncryptCustomSNonce(
    sender: Short,
    receiver: Short,
    val sNonceFirst: Int,
    val sNonceSecond: Int,
) : ConnectionMessageTypes {
    val header: ConnPacketHeader =
        ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_SNONCE, sender, receiver)
    private val sNonce: Array<Int> = arrayOf(sNonceFirst, sNonceSecond)

    companion object {
        const val SIZEOF_PACKET =
            ConnPacketHeader.SIZEOF_PACKET + 8
    }

    override fun createBytePacket(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(ConnPacketEncryptCustomSNonce.SIZEOF_PACKET).order(
            ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(header.createBytePacket())
        byteBuffer.putInt(sNonce[0])
        byteBuffer.putInt(sNonce[1])
        return byteBuffer.array()
    }
}

class ConnPacketEncryptCustomANonce(
    sender: Short,
    receiver: Short,
    val aNonceFirst: Int,
    val aNonceSecond: Int,
) : ConnectionMessageTypes {
    val header: ConnPacketHeader =
        ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_SNONCE, sender, receiver)
    private val aNonce: Array<Int> = arrayOf(aNonceFirst, aNonceSecond)

    companion object {
        const val SIZEOF_PACKET = ConnPacketHeader.SIZEOF_PACKET + 8
        fun readFromBytePacket(bytePacket: ByteArray): ConnPacketEncryptCustomANonce? {
            if (bytePacket.size < SIZEOF_PACKET) return null
            val byteBuffer = ByteBuffer.wrap(bytePacket).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.get() // skip header(1byte)
            return ConnPacketEncryptCustomANonce(
                byteBuffer.short, byteBuffer.short, byteBuffer.int, byteBuffer.int)
        }
    }

    override fun createBytePacket(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(ConnPacketEncryptCustomSNonce.SIZEOF_PACKET).order(
            ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(header.createBytePacket())
        byteBuffer.putInt(aNonce[0])
        byteBuffer.putInt(aNonce[1])
        return byteBuffer.array()
    }
}


enum class MessageType(val typeValue: Byte) {
    INVALID(0), SPLIT_WRITE_CMD(16),  //Used if a WRITE_CMD message is split
    SPLIT_WRITE_CMD_END(17),  //Used if a WRITE_CMD message is split
    CLUSTER_WELCOME(20),  //The initial message after a connection setup ((byte)Sent between two nodes)
    CLUSTER_ACK_1(21),  //Both sides must acknowledge the handshake ((byte)Sent between two nodes)
    CLUSTER_ACK_2(22),  //Second ack ((byte)Sent between two nodes)
    CLUSTER_INFO_UPDATE(23),  //When the cluster size changes), this message is used ((byte)Sent to all nodes)
    RECONNECT(24),  //Sent while trying to reestablish a connection
    ENCRYPT_CUSTOM_START(25), ENCRYPT_CUSTOM_ANONCE(26), ENCRYPT_CUSTOM_SNONCE(27), ENCRYPT_CUSTOM_DONE(
        28),
    UPDATE_TIMESTAMP(30),  //Used to enable timestamp distribution over the mesh
    UPDATE_CONNECTION_INTERVAL(31),  //Instructs a node to use a different connection interval
    ASSET_LEGACY(32), CAPABILITY(33), ASSET_GENERIC(34), SIG_MESH_SIMPLE(
        35),  //A lightweight wrapper for SIG mesh access layer messages
    MODULE_MESSAGES_START(50), MODULE_CONFIG(50),  //Used for many different messages that set and get the module config
    MODULE_TRIGGER_ACTION(51),  //Trigger some custom module action
    MODULE_ACTION_RESPONSE(52),  //Response on a triggered action
    MODULE_GENERAL(53),  //A message), generated by the module not as a response to an action), e.g. an event
    MODULE_RAW_DATA(54), MODULE_RAW_DATA_LIGHT(55), COMPONENT_ACT(58),  //Actuator messages
    COMPONENT_SENSE(59),  //Sensor messages
    MODULE_MESSAGES_END(59), TIME_SYNC(60), DEAD_DATA(61),  //Used by the MeshAccessConnection when malformed data was received.
    DATA_1(80), DATA_1_VITAL(81), CLC_DATA(83), RESERVED_BIT_START(128.toByte()), RESERVED_BIT_END(
        255.toByte());

    companion object {
        fun getMessageType(typeValue: Byte): MessageType {
            return values().find { it.typeValue == typeValue }
                ?: INVALID
        }
    }
}

// TODO change INT to UINT
enum class FmKeyId(val keyId: Int) {
    ZERO(0), NODE(1), NETWORK(2), BASE_USER(3), ORGANIZATION(4), RESTRAINED(5), USER_DERIVED_START(
        10),
    USER_DERIVED_END(Int.MAX_VALUE);

    companion object {
        fun getFmKeyId(keyId: Int): FmKeyId {
            return values().find { it.keyId == keyId } ?: USER_DERIVED_END
        }
    }
}