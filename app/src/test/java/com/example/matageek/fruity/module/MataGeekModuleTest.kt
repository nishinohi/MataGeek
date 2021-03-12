package com.example.matageek.fruity.module

import junit.framework.TestCase
import org.hamcrest.CoreMatchers.`is`

import org.junit.Assert.*
import org.junit.Test
import java.lang.Exception

class MataGeekModuleTest : TestCase() {
    @Test
    fun test_read_statusReporterModuleStatusMessage() {
        val packet = byteArrayOf(
            0x03, 0x00, 0x64, 0x00, 0x10, 0x03, 0x02, 0x01, 0x05
        )
        val message =
            StatusReporterModule.StatusReporterModuleStatusMessage.readFromBytePacket(packet)?:throw Exception("invalid")
        assertThat(message.clusterSize, `is`(3))
        assertThat(message.inConnectionPartner, `is`(100))
        assertThat(message.inConnectionRSSI, `is`(16))
        assertThat(message.freeInAndOut, `is`(3))
        assertThat(message.batteryInfo, `is`(2))
        assertThat(message.connectionLossCounter, `is`(1))
        assertThat(message.initializedByGateway, `is`(5))
    }

    @Test
    fun test_List() {
        val temp = mutableListOf<Byte>()
        val packet = byteArrayOf(
            0x03, 0x00, 0x64, 0x00, 0x10, 0x03, 0x02, 0x01, 0x05
        )
        temp.addAll(packet.toList())
        assertThat(temp.size, `is`(9))
        temp.clear()
        temp.addAll(packet.copyOfRange(2, packet.size).toList())
        assertThat(temp.size, `is`(7))
    }
}