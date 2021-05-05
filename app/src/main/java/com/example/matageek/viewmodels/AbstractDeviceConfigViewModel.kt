package com.example.matageek.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.matageek.adapter.DiscoveredDevice
import com.example.matageek.fruity.module.MatageekModule
import com.example.matageek.fruity.module.StatusReporterModule
import com.example.matageek.fruity.types.*
import com.example.matageek.manager.CommonDeviceInfo
import com.example.matageek.manager.DeviceInfo
import com.example.matageek.manager.DeviceInfoObserver
import com.example.matageek.manager.MeshAccessManager
import kotlinx.coroutines.*
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import kotlin.coroutines.resume

abstract class AbstractDeviceConfigViewModel(application: Application) :
    AndroidViewModel(application), DeviceInfoObserver {
    protected val meshAccessManager: MeshAccessManager = MeshAccessManager(application)

    /** Device Connection State */
    val connectionState: LiveData<ConnectionState> = meshAccessManager.state
    val handShakeState = meshAccessManager.handShakeState
    val progressState: MutableLiveData<Boolean> = MutableLiveData()

    /** Device Info */
    var displayNodeId: Short = 0
    val clusterSize: MutableLiveData<Short> = MutableLiveData()
    val batteryInfo: MutableLiveData<Byte> = MutableLiveData()
    val trapState: MutableLiveData<Boolean> = MutableLiveData()
    val modeState: MutableLiveData<MatageekModule.MatageekMode> = MutableLiveData()
    val deviceName: MutableLiveData<String> = MutableLiveData()

    /** Mesh Graph */
    val meshGraph = MeshGraph(meshAccessManager.getPartnerId())
    val nodeIdList: MutableLiveData<List<Short>> = MutableLiveData()

    private lateinit var discoveredDevice: DiscoveredDevice

    fun addDeviceInfoObserver() {
        meshAccessManager.addDeviceInfoObserver(this)
    }

    fun connect(discoveredDevice: DiscoveredDevice) {
        this.discoveredDevice = discoveredDevice
        reconnect(discoveredDevice.device)
    }

    // TODO replace magic number
    private fun reconnect(device: BluetoothDevice) {
        meshAccessManager.connect(device).retry(3, 100)
            .useAutoConnect(false)
            .enqueue()
    }

    fun startHandShake() {
        if (connectionState.value == ConnectionState.Ready) {
            meshAccessManager.startEncryptionHandshake(discoveredDevice.enrolled)
        }
    }

    // TODO implement failed
    fun disconnect() {
        meshAccessManager.disconnect().enqueue()
    }

    fun inProgress() {
        progressState.postValue(true)
    }

    fun endProgress() {
        progressState.postValue(false)
    }

    private suspend fun sendModuleActionTriggerMessageAsync(
        targetNodeId: Short, moduleIdWrapper: ModuleIdWrapper, triggerActionType: Byte,
        responseActionType: Byte, counter: Short,
        customCallback: ((packet: ByteArray) -> Unit)? = null,
    ): Boolean {
        return suspendCancellableCoroutine {
            meshAccessManager.sendModuleActionTriggerMessage(moduleIdWrapper,
                triggerActionType, targetNodeId)
            meshAccessManager.addTimeoutJob(moduleIdWrapper, responseActionType, 0, counter,
                { it.resume(true) }, customCallback)
        }
    }

    fun updateMatageekStatus(
        targetNodeId: Short = meshAccessManager.getPartnerId(),
        failedCallback: (() -> Unit)? = null, timeoutMillis: Long = 5000,
    ) {
        viewModelScope.launch {
            withTimeout(timeoutMillis) {
                try {
                    sendModuleActionTriggerMessageAsync(
                        targetNodeId,
                        ModuleIdWrapper.generateVendorModuleIdWrapper(VendorModuleId.MATAGEEK_MODULE.id,
                            1),
                        MatageekModule.MatageekModuleTriggerActionMessages.STATE.type,
                        MatageekModule.MatageekModuleActionResponseMessages.STATE_RESPONSE.type,
                        1) { packet ->
                        val trapStateMessage =
                            MatageekModule.MatageekModuleStateMessage(packet.copyOfRange(
                                ConnPacketVendorModule.SIZEOF_PACKET, packet.size))
                        updateDisplayDeviceInfo(DeviceInfo(targetNodeId,
                            null, null, trapStateMessage.trapState,
                            null, MatageekModule.MatageekMode.getMode(trapStateMessage.mode)))
                    }
                } catch (e: TimeoutCancellationException) {
                    failedCallback?.let { it() }
                }
            }
        }
    }

    fun updateDeviceInfo(
        targetNodeId: Short = meshAccessManager.getPartnerId(),
        successCallback: (() -> Unit)? = null,
        failedCallback: (() -> Unit)? = null, timeoutMillis: Long = 5000,
    ) {
        viewModelScope.launch {
            withTimeout(timeoutMillis) {
                try {
                    sendModuleActionTriggerMessageAsync(targetNodeId,
                        ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id),
                        StatusReporterModule.StatusModuleTriggerActionMessages.GET_STATUS.type,
                        StatusReporterModule.StatusModuleActionResponseMessages.STATUS.type,
                        1) { packet ->
                        val statusMessage =
                            StatusReporterModule.StatusReporterModuleStatusMessage.readFromBytePacket(
                                packet.copyOfRange(ConnPacketModule.SIZEOF_PACKET, packet.size))
                        updateDisplayDeviceInfo(DeviceInfo(targetNodeId,
                            statusMessage.clusterSize, statusMessage.batteryInfo))
                        successCallback?.let { it() }
                    }
                } catch (e: TimeoutCancellationException) {
                    failedCallback?.let { it() }
                }
            }
        }
    }

    fun updateMeshGraph(failedCallback: (() -> Unit)? = null, timeoutMillis: Long = 5000) {
        viewModelScope.launch {
            withTimeout(timeoutMillis) {
                meshGraph.initialize(meshAccessManager.getPartnerId())
                try {
                    sendModuleActionTriggerMessageAsync(PrimitiveTypes.NODE_ID_BROADCAST,
                        ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id),
                        StatusReporterModule.StatusModuleTriggerActionMessages.GET_ALL_CONNECTIONS.type,
                        StatusReporterModule.StatusModuleActionResponseMessages.ALL_CONNECTIONS.type,
                        clusterSize.value ?: 1) { packet ->
                        val sender = ConnPacketModule(packet).header.sender
                        val conStatus = StatusReporterModule.StatusReporterModuleConnectionsMessage(
                            packet.copyOfRange(ConnPacketModule.SIZEOF_PACKET, packet.size))

                        val addNodeToMesh = fun(_sender: Short, _partner: Short) {
                            if (_partner != 0.toShort()) meshGraph.addNode(_sender, _partner)
                        }
                        addNodeToMesh(sender, conStatus.partner1)
                        addNodeToMesh(sender, conStatus.partner2)
                        addNodeToMesh(sender, conStatus.partner3)
                        addNodeToMesh(sender, conStatus.partner4)
                    }
                    nodeIdList.postValue(meshGraph.nodeIdMap.keys.toList())
                    Log.d("MATAG", "updateMeshGraph: ")
                } catch (e: TimeoutCancellationException) {
                    failedCallback?.let { it() }
                }
            }
        }
    }

    override fun updateDisplayDeviceInfo(deviceInfo: DeviceInfo) {
        if (deviceInfo.nodeId != displayNodeId) return
        deviceInfo.clusterSize?.let { clusterSize.postValue(it) }
        deviceInfo.batteryInfo?.let { batteryInfo.postValue(it) }
        deviceInfo.trapState?.let { trapState.postValue(it) }
        deviceInfo.matageekMode?.let { modeState.postValue(it) }
        deviceInfo.deviceName?.let { deviceName.postValue(it) }
    }

    override fun updateCommonDeviceInfo(commonDeviceInfo: CommonDeviceInfo) {
        commonDeviceInfo.clusterSize?.let { clusterSize.postValue(it) }
        commonDeviceInfo.matageekMode?.let { modeState.postValue(it) }
    }

    fun updateDisplayNodeIdByPartnerId() {
        displayNodeId = meshAccessManager.getPartnerId()
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }

}