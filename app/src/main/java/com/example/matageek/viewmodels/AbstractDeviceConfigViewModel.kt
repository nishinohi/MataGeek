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
    val currentNodeId: MutableLiveData<Short> = MutableLiveData()
    val clusterSize: MutableLiveData<Short> = MutableLiveData()
    val batteryInfo: MutableLiveData<Byte> = MutableLiveData()
    val trapState: MutableLiveData<Boolean> = MutableLiveData()
    val modeState: MutableLiveData<MatageekModule.MatageekMode> = MutableLiveData()
    val deviceName: MutableLiveData<String> = MutableLiveData()

    /** Mesh Graph */
    val meshGraph = MeshGraph(meshAccessManager.getPartnerId())

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

    private suspend fun sendGetAllConnectionAsync(customCallback: (packet: ByteArray) -> Unit): Boolean {
        return suspendCancellableCoroutine {
            val statusModuleIdWrapper = ModuleIdWrapper(ModuleId.STATUS_REPORTER_MODULE.id)
            meshAccessManager.sendModuleActionTriggerMessage(statusModuleIdWrapper,
                StatusReporterModule.StatusModuleTriggerActionMessages.GET_ALL_CONNECTIONS.type,
                PrimitiveTypes.NODE_ID_BROADCAST)
            meshAccessManager.addTimeoutJob(statusModuleIdWrapper,
                StatusReporterModule.StatusModuleTriggerActionMessages.GET_ALL_CONNECTIONS.type,
                0, clusterSize.value ?: 0, { it.resume(true) }, customCallback)
        }
    }

    fun updateMeshGraph(timeoutMillis: Long = 5000, failedCallback: (() -> Unit)? = null) {
        viewModelScope.launch {
            withTimeout(timeoutMillis) {
                meshGraph.clear()
                meshGraph.rootNodeId = meshAccessManager.getPartnerId()
                sendGetAllConnectionAsync(fun(packet: ByteArray) {
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
                })
                Log.d("MATAG", "updateMeshGraph: ")
            }
        }
    }

    override fun updateDeviceInfo(deviceInfo: DeviceInfo) {
        deviceInfo.nodeId?.let { currentNodeId.postValue(it) }
        deviceInfo.clusterSize?.let { clusterSize.postValue(it) }
        deviceInfo.batteryInfo?.let { batteryInfo.postValue(it) }
        deviceInfo.trapState?.let { trapState.postValue(it) }
        deviceInfo.matageekMode?.let { modeState.postValue(it) }
        deviceInfo.deviceName?.let { deviceName.postValue(it) }
    }

    fun updateCurrentNodeIdByPartnerId() {
        currentNodeId.postValue(meshAccessManager.getPartnerId())
    }

    companion object {
        const val EXTRA_DEVICE: String = "com.matageek.EXTRA_DEVICE"
    }

}