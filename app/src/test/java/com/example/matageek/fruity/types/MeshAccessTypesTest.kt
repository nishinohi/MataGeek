package com.example.matageek.fruity.types

import junit.framework.TestCase
import org.hamcrest.CoreMatchers.`is`

import org.junit.Assert.*
import org.junit.Test

class MeshAccessTypesTest : TestCase() {
    @Test
    fun test_AdvStructureMeshAccessServiceData() {
        val adv = AdvStructureMeshAccessServiceData(
            AdvStructureServiceDataAndType(
                AdvStructureUUID16(0, 0, 0), 0, 0
            ),
            0,
            true,
            false,
            false,
            false,
            0,
            0,
            0,
            byteArrayOf(0, 0, 0),
            AdvStructureMeshAccessServiceData.PotentiallySlicedOff(0,
                byteArrayOf(0, 0, 0),
                byteArrayOf(0, 0, 0, 0))
        )

        val packet = byteArrayOf(0x17, 0x16, 0x12, 0xfe.toByte(), 0x3, 0x0,
            0x0, 0xb, 0xC1.toByte(), 0xe9.toByte(), 0xd0.toByte(), 0x28, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0)

        val advStr = AdvStructureMeshAccessServiceData(packet)
        assertThat(advStr.data.uuid.len, `is`(23))
        assertThat(advStr.data.uuid.type, `is`(22))
        assertThat(advStr.data.uuid.uuid, `is`(65042.toShort()))
        assert(advStr.isEnrolled)
        assert(!advStr.isSink)
        assert(!advStr.isZeroKeyConnectable)
        assert(!advStr.isConnectable)
        assertThat(advStr.interestedInConnection, `is`(0))
        assertThat(advStr.reserved, `is`(6))
    }
}