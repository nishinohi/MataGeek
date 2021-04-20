package com.example.matageek.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.matageek.customexception.MessagePacketSizeException
import com.example.matageek.fruity.module.EnrollmentModule
import com.example.matageek.fruity.types.ConnPacketModule
import com.example.matageek.fruity.types.ModuleId
import com.example.matageek.fruity.types.ModuleIdWrapper
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

class DeviceNonActivatedViewModel(application: Application) :
    AbstractDeviceConfigViewModel(application) {

    val enrolledNodeId: MutableList<Short> = mutableListOf()

    // TODO When enroll process success, device will reset so not all response can receive
    // so timeout map counter have to be 0
    private suspend fun sendEnrollmentBroadcastAppStartAsync(): Boolean {
        return suspendCancellableCoroutine {
            meshAccessManager.sendEnrollmentBroadcastAppStart()
            meshAccessManager.addTimeoutJob(
                ModuleIdWrapper(ModuleId.ENROLLMENT_MODULE.id).wrappedModuleId,
                EnrollmentModule.EnrollmentModuleActionResponseMessages.ENROLLMENT_RESPONSE.type,
                0, clusterSize.value!!, { it.resume(true) }) {
                addEnrolledNodeId(it)
            }
        }
    }

    private fun addEnrolledNodeId(packet: ByteArray) {
        if (packet.size < ConnPacketModule.SIZEOF_PACKET + EnrollmentModule.EnrollmentModuleEnrollmentResponse.SIZE_OF_PACKET) {
            throw MessagePacketSizeException(EnrollmentModule.EnrollmentModuleEnrollmentResponse::class.java.toString(),
                ConnPacketModule.SIZEOF_PACKET + EnrollmentModule.EnrollmentModuleEnrollmentResponse.SIZE_OF_PACKET)
        }
        val enrollResponse = EnrollmentModule.EnrollmentModuleEnrollmentResponse(
            packet.copyOfRange(ConnPacketModule.SIZEOF_PACKET,
                ConnPacketModule.SIZEOF_PACKET + EnrollmentModule.EnrollmentModuleEnrollmentResponse.SIZE_OF_PACKET)
        )
        if (enrollResponse.enrollmentResponseCode != EnrollmentModule.EnrollmentResponseCode.OK.code) return
        val connPacketModule =
            ConnPacketModule(packet.copyOfRange(0, ConnPacketModule.SIZEOF_PACKET))
        enrolledNodeId.add(connPacketModule.header.sender)
    }

    suspend fun sendEnrollmentBroadcastAppStart(
        successCallback: (() -> Unit)? = null,
        failCallback: (() -> Unit)? = null,
        timeMills: Long = 5000,
    ) {
        enrolledNodeId.clear()
        val result = withContext(viewModelScope.coroutineContext) {
            withTimeout(timeMills) {
                try {
                    sendEnrollmentBroadcastAppStartAsync()
                } catch (e: TimeoutCancellationException) {
                    failCallback?.let { it() }
                    false
                }
            }
        }
        if (result) successCallback?.let { it() }
    }

}