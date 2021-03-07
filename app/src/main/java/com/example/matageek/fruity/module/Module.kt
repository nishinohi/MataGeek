package com.example.matageek.fruity.module

import com.example.matageek.fruity.types.ConnPacketModule
import com.example.matageek.fruity.types.MessageType
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.MeshAccessManager
import com.example.matageek.manager.MeshAccessObserver
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class Module(protected var moduleName: String, var moduleId: Byte) {
    private val observers: MutableList<MeshAccessObserver> = mutableListOf()

    fun addObserver(observer: MeshAccessObserver) {
        observers.add(observer)
    }

    fun deleteObserver(observer: MeshAccessObserver) {
        observers.remove(observer)
    }

    fun notifyObserver(deviceInfo: DeviceInfo) {
        observers.forEach {
            it.update(deviceInfo)
        }
    }

    //TODO: reliable is currently not supported and by default false. The input is ignored
    protected fun createSendModuleActionMessagePacket(
        messageType: MessageType, receiver: Short, requestHandle: Byte, actionType: Byte,
        additionalData: ByteArray?, additionalDataSize: Int, reliable: Boolean,
    ): ByteArray {
        val packetBuf = ByteBuffer.allocate(ConnPacketModule.SIZEOF_PACKET + additionalDataSize)
            .order(ByteOrder.LITTLE_ENDIAN)
        packetBuf.put(messageType.typeValue)
        packetBuf.putShort(MeshAccessManager.NODE_ID)
        packetBuf.putShort(receiver)
        packetBuf.put(moduleId)
        packetBuf.put(requestHandle)
        packetBuf.put(actionType)
        if (additionalData != null) packetBuf.put(additionalData)
        return packetBuf.array()
    }

    abstract fun actionResponseMessageReceivedHandler(packet: ByteArray)

}
