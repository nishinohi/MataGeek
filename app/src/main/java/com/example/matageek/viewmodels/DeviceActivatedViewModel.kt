package com.example.matageek.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.matageek.fruity.module.MatageekModule
import com.example.matageek.fruity.types.ModuleIdWrapper
import com.example.matageek.fruity.types.PrimitiveTypes
import com.example.matageek.fruity.types.VendorModuleId
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

class DeviceActivatedViewModel(application: Application) :
    AbstractDeviceConfigViewModel(application) {

    var newMode: MatageekModule.MatageekMode = MatageekModule.MatageekMode.SETUP

    private suspend fun sendMatageekModeChangeMessageAsync(): Boolean {
        return suspendCancellableCoroutine {
            if (modeState.value == null || clusterSize.value == null) it.resume(false)
            val newMode = if (modeState.value!! == MatageekModule.MatageekMode.SETUP)
                MatageekModule.MatageekMode.DETECT else MatageekModule.MatageekMode.SETUP
            this.newMode = newMode
            val data: MatageekModule.MatageekModuleModeChangeMessage =
                MatageekModule.MatageekModuleModeChangeMessage(newMode, clusterSize.value!!)
            val matageekModuleIdWrapper =
                ModuleIdWrapper.generateVendorModuleIdWrapper(VendorModuleId.MATAGEEK_MODULE.id, 1)
            meshAccessManager.sendModuleActionTriggerMessage(
                matageekModuleIdWrapper,
                MatageekModule.MatageekModuleTriggerActionMessages.MODE_CHANGE.type,
                PrimitiveTypes.NODE_ID_BROADCAST, data.createBytePacket(),
                MatageekModule.MatageekModuleModeChangeMessage.SIZEOF_PACKET)

            meshAccessManager.addTimeoutJob(
                matageekModuleIdWrapper,
                MatageekModule.MatageekModuleActionResponseMessages.MODE_CHANGE_RESPONSE.type,
                0, clusterSize.value!!, { it.resume(true) })
        }
    }

    suspend fun sendMatageekModeChangeMessage(timeoutMillis: Long = 5000) {
        val result = withContext(viewModelScope.coroutineContext) {
            withTimeout(timeoutMillis) {
                try {
                    sendMatageekModeChangeMessageAsync()
                } catch (e: TimeoutCancellationException) {
                    val matageekModuleId = ModuleIdWrapper.generateVendorModuleIdWrapper(
                        VendorModuleId.MATAGEEK_MODULE.id, 1)
                    val key = meshAccessManager.generateTimeoutKey(matageekModuleId,
                        MatageekModule.MatageekModuleActionResponseMessages.MODE_CHANGE_RESPONSE.type,
                        0)
                    Log.d("MATAG", "timeout and delete key: $key")
                    meshAccessManager.timeoutMap.remove(key)
                    false
                }
            }
        }
        if (result) modeState.postValue(newMode)
    }
}