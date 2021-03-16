package com.example.matageek.fruity.types

import junit.framework.TestCase
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class ConnectionMessageTypesTest : TestCase() {
    @Test
    fun test_connPacketHeader_packet() {
        val connPacketHeader = ConnPacketHeader(MessageType.ENCRYPT_CUSTOM_START, 100, 101)
        val expect: ByteArray = byteArrayOf(0x19, 0x64, 0x00, 0x65, 0x00)
        assertThat(connPacketHeader.createBytePacket(), `is`(expect))
    }

    @Test
    fun test_connPacketEncryptCustomStart_packet() {
        val packet = ConnPacketEncryptCustomStart(259, 100, 1,
            FmKeyId.NODE, 1, 57)
        val expect: ByteArray =
            byteArrayOf(0x19, 0x03, 0x01, 0x64, 0x00, 0x01, 0x01, 0x00, 0x00, 0x00, 0xE5.toByte())
        assertThat(packet.createBytePacket(), `is`(expect))
    }

    @Test
    fun test_connPacketEncryptCustomSNonce_packet() {
        val sNonce = intArrayOf(1689834492, 434638765)
        val packet: ConnPacketEncryptCustomSNonce =
            ConnPacketEncryptCustomSNonce(1, 2, sNonce[0], sNonce[1])
        val expect: ByteArray =
            byteArrayOf(0x1B, 0x01, 0x00, 0x02, 0x00, 0xFC.toByte(),
                0xD3.toByte(), 0xB8.toByte(), 0x64, 0xAD.toByte(), 0x0F, 0xE8.toByte(), 0x19)
        assertThat(packet.createBytePacket(), `is`(expect))
    }

    @Test
    fun test_read_connPacketEncryptCustomANonce() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val packet = byteArrayOf(0x1B, 0x64, 0x00, 0x65, 0x00, 0x1D, 0x4C,
            0xFA.toByte(), 0x4E, 0x32, 0x19, 0x68, 0x2A)
        val convert = ConnPacketEncryptCustomANonce(packet)
        assertNotNull(convert)
        assertNotNull(convert?.header)
        assertThat(convert?.header?.sender, `is`(100))
        assertThat(convert?.header?.receiver, `is`(101))
        assertThat(convert?.aNonceFirst, `is`(1325026333))
        assertThat(convert?.aNonceSecond, `is`(711465266))
    }

    @Test
    fun test_read_ConnPacketHeader() {
        val packet = byteArrayOf(0x1C, 0x65, 0x00, 0x00, 0x7D)
        val header = ConnPacketHeader(packet)
        assertNotNull(header)
        assertThat(header?.messageType, `is`(MessageType.ENCRYPT_CUSTOM_DONE))
        assertThat(header?.sender, `is`(101))
        assertThat(header?.receiver, `is`(32000))
    }
}