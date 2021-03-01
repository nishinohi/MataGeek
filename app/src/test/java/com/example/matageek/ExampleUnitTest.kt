package com.example.matageek

import com.example.matageek.fruity.types.ConnPacketHeader
import com.example.matageek.fruity.types.MessageType
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun createPacket() {
        val connPacketHeader = ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_START, 24, 100)
        val packet: ByteArray = connPacketHeader.createBytePacket()
        assertEquals(1, 1 + 0)
    }
}

