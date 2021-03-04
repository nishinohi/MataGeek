package com.example.matageek.fruity.module

import com.example.matageek.fruity.types.ConnPacketModule
import com.example.matageek.fruity.types.MessageType
import com.example.matageek.manager.MataGeekBleManager
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class Module(protected var moduleName: String, private var moduleId: Byte) {
    //TODO: reliable is currently not supported and by default false. The input is ignored
    protected fun createSendModuleActionMessagePacket(
        messageType: MessageType, receiver: Short, requestHandle: Byte, actionType: Byte,
        additionalData: ByteArray?, additionalDataSize: Int, reliable: Boolean,
    ): ByteArray {
        val packetBuf = ByteBuffer.allocate(ConnPacketModule.SIZEOF_PACKET + additionalDataSize)
            .order(ByteOrder.LITTLE_ENDIAN)
        packetBuf.put(messageType.typeValue)
        packetBuf.putShort(MataGeekBleManager.NODE_ID)
        packetBuf.putShort(receiver)
        packetBuf.put(moduleId)
        packetBuf.put(requestHandle)
        packetBuf.put(actionType)
        if (additionalData != null) packetBuf.put(additionalData)
        return packetBuf.array()
    }
}
