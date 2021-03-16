package com.example.matageek.fruity.module

import com.example.matageek.fruity.types.ConnPacketModule
import com.example.matageek.fruity.types.ConnPacketVendorModule
import com.example.matageek.fruity.types.MessageType
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.MeshAccessManager
import com.example.matageek.manager.MeshAccessObserver
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class Module(
    protected val moduleName: String,
    val moduleId: Byte,
    val vendorModuleId: Int = 0,
) {

    constructor(moduleName: String, vendorModuleId: Int) : this(moduleName, 0, vendorModuleId)

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

    private fun isVendorModule(): Boolean {
        return moduleId == 0.toByte()
    }

    //TODO: reliable is currently not supported and by default false. The input is ignored
    protected fun createSendModuleActionMessagePacket(
        messageType: MessageType, receiver: Short, requestHandle: Byte, actionType: Byte,
        additionalData: ByteArray?, additionalDataSize: Int, reliable: Boolean,
    ): ByteArray {
        val headerSize =
            if (isVendorModule()) ConnPacketVendorModule.SIZEOF_PACKET else ConnPacketModule.SIZEOF_PACKET
        val packetBuf =
            ByteBuffer.allocate(headerSize + additionalDataSize).order(ByteOrder.LITTLE_ENDIAN)

        if (isVendorModule()) {
            val connPacketVendorModule = ConnPacketVendorModule(
                messageType, MeshAccessManager.NODE_ID, receiver, vendorModuleId,
                requestHandle, actionType)
            packetBuf.put(connPacketVendorModule.createBytePacket())
        } else {
            val connPacketModule = ConnPacketModule(
                messageType, MeshAccessManager.NODE_ID, receiver, moduleId,
                requestHandle, actionType)
            packetBuf.put(connPacketModule.createBytePacket())
        }
        if (additionalData != null) packetBuf.put(additionalData)
        return packetBuf.array()
    }

    fun createTriggerActionMessagePacket(
        receiver: Short, actionType: Byte, additionalData: ByteArray? = null,
        additionalDataSize: Int = 0,
    ): ByteArray {
        return createSendModuleActionMessagePacket(MessageType.MODULE_TRIGGER_ACTION, receiver, 0,
            actionType, additionalData, additionalDataSize, false)
    }

    abstract fun actionResponseMessageReceivedHandler(packet: ByteArray)

}
