package com.example.matageek.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.example.matageek.fruity.module.MatageekModule
import com.example.matageek.fruity.types.PrimitiveTypes
import com.example.matageek.fruity.types.VendorModuleId

class DeviceActivatedViewModel(application: Application) :
    AbstractDeviceConfigViewModel(application) {

    val deviceName: MutableLiveData<String> = MutableLiveData()
    val clusterSize = meshAccessManager.clusterSize
    val battery = meshAccessManager.batteryInfo
    val trapState = meshAccessManager.trapState
    val mode = meshAccessManager.modeState

    fun sendMatageekModeChangeMessage() {
        if (mode.value == null || clusterSize.value == null) return
        val newMode = if (mode.value!! == MatageekModule.MatageekMode.SETUP)
            MatageekModule.MatageekMode.DETECT else MatageekModule.MatageekMode.SETUP
        val data: MatageekModule.MatageekModuleModeChangeMessage =
            MatageekModule.MatageekModuleModeChangeMessage(newMode, clusterSize.value!!)

        meshAccessManager.sendModuleActionTriggerMessage(
            PrimitiveTypes.getVendorModuleId(VendorModuleId.MATAGEEK_MODULE.id, 1),
            MatageekModule.MatageekModuleTriggerActionMessages.MODE_CHANGE.type,
            PrimitiveTypes.NODE_ID_BROADCAST, data.createBytePacket(),
            MatageekModule.MatageekModuleModeChangeMessage.SIZEOF_PACKET)
    }

    fun deviceNameUpdate(deviceName: String) {
        this.deviceName.postValue(deviceName)
    }

}