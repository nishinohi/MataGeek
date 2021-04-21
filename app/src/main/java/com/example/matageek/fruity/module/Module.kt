package com.example.matageek.fruity.module

import com.example.matageek.fruity.types.ConnPacketModule
import com.example.matageek.fruity.types.ConnPacketVendorModule
import com.example.matageek.fruity.types.MessageType
import com.example.matageek.fruity.types.ModuleIdWrapper
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.MeshAccessManager
import com.example.matageek.manager.DeviceInfoObserver
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class Module(
    protected val moduleName: String,
    val moduleIdWrapper: ModuleIdWrapper,
) {
    private val observers: MutableList<DeviceInfoObserver> = mutableListOf()

    fun addObserver(observer: DeviceInfoObserver) {
        observers.add(observer)
    }

    fun deleteObserver(observer: DeviceInfoObserver) {
        observers.remove(observer)
    }

    fun notifyObserver(deviceInfo: DeviceInfo) {
        observers.forEach {
            it.updateDeviceInfo(deviceInfo)
        }
    }

    //TODO: reliable is currently not supported and by default false. The input is ignored
    protected fun createSendModuleActionMessagePacket(
        messageType: MessageType, receiver: Short, requestHandle: Byte, actionType: Byte,
        additionalData: ByteArray?, additionalDataSize: Int, reliable: Boolean,
    ): ByteArray {
        val headerSize =
            if (moduleIdWrapper.isVendorModuleId) ConnPacketVendorModule.SIZEOF_PACKET else ConnPacketModule.SIZEOF_PACKET
        val packetBuf =
            ByteBuffer.allocate(headerSize + additionalDataSize).order(ByteOrder.LITTLE_ENDIAN)

        if (moduleIdWrapper.isVendorModuleId) {
            val connPacketVendorModule = ConnPacketVendorModule(
                messageType, MeshAccessManager.NODE_ID, receiver, moduleIdWrapper.wrappedModuleId,
                requestHandle, actionType)
            packetBuf.put(connPacketVendorModule.createBytePacket())
        } else {
            val connPacketModule = ConnPacketModule(
                messageType, MeshAccessManager.NODE_ID, receiver, moduleIdWrapper.primaryModuleId,
                requestHandle, actionType)
            packetBuf.put(connPacketModule.createBytePacket())
        }
        if (additionalData != null) packetBuf.put(additionalData)
        return packetBuf.array()
    }

    fun createTriggerActionMessagePacket(
        receiver: Short, actionType: Byte, additionalData: ByteArray? = null,
        additionalDataSize: Int = 0, requestHandle: Byte = 0,
    ): ByteArray {
        return createSendModuleActionMessagePacket(MessageType.MODULE_TRIGGER_ACTION,
            receiver, requestHandle, actionType, additionalData, additionalDataSize, false)
    }

    abstract fun actionResponseMessageReceivedHandler(packet: ByteArray)

    companion object {
        fun isVendorModuleId(moduleId: Int): Boolean {
            return true
        }
    }

}
