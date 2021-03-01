package com.example.matageek.profile

import com.example.matageek.profile.callback.MeshAccessDataCallback
import junit.framework.TestCase
import no.nordicsemi.android.ble.data.Data
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class EncryptionTest : TestCase() {
    @Test
    fun test_encrypt_packet_with_mic() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val encryptNonce: Array<Int> = arrayOf(1325026333, 711465266)
        val plainText = byteArrayOf(0x1B, 0x01, 0x00, 0x02, 0x00, 0xFC.toByte(), 0xD3.toByte(),
            0xB8.toByte(), 0x64, 0xAD.toByte(), 0x0F, 0xE8.toByte(), 0x19, 0x00, 0x00, 0x00)
        val encryptKey: SecretKey =
            SecretKeySpec(byteArrayOf(0x03, 0x1C, 0xBD.toByte(), 0xBA.toByte(), 0x73, 0x42,
                0xFD.toByte(), 0xB0.toByte(), 0x95.toByte(), 0x13, 0x81.toByte(), 0xAB.toByte(),
                0x97.toByte(), 0x94.toByte(), 0x8C.toByte(), 0xD9.toByte()), "AES")
        val expect =
            byteArrayOf(0x79.toByte(), 0x65.toByte(), 0xA5.toByte(), 0xB6.toByte(), 0xA6.toByte(),
                0xA7.toByte(), 0x58.toByte(), 0x89.toByte(), 0x0D.toByte(), 0xE8.toByte(),
                0x77.toByte(), 0xED.toByte(), 0xDC.toByte(), 0xCA.toByte(), 0xCA.toByte(),
                0x47.toByte(), 0x57.toByte())
        assertThat(FruityDataEncryptAndSplit(encryptNonce, encryptKey).encryptPacketWithMIC(
            plainText,
            13,
            encryptNonce,
            encryptKey), `is`(expect))
    }

    @Test
    fun test_split_chunk_length() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val encryptNonce = arrayOf(1325026333, 711465266)
        val encryptKey: SecretKey = SecretKeySpec(byteArrayOf(0x03, 0x1C, 0xBD.toByte(),
            0xBA.toByte(), 0x73, 0x42, 0xFD.toByte(), 0xB0.toByte(), 0x95.toByte(), 0x13,
            0x81.toByte(), 0xAB.toByte(), 0x97.toByte(), 0x94.toByte(), 0x8C.toByte(),
            0xD9.toByte()), "AES")
        // length = 30
        val plainText = byteArrayOf(0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01)
        val splitter = FruityDataEncryptAndSplit(encryptNonce, encryptKey)
        var index = 0
        var target = ByteArray(0)
        var splitData: ByteArray? = ByteArray(0)
        while (splitData != null) {
            splitData = splitter.chunk(plainText, index++, 23)
            if (splitData != null) {
                target = ByteArray(splitData.size)
                System.arraycopy(splitData, 0, target, 0, splitData.size)
            }
        }
        // encrypted packet payload max size is 14
        // encrypted packet split header and mic total size is 6
        // target = 30 - 14 - 14 + 6 = 8
        assertThat(target.size, `is`(8))
    }

    @Test
    fun test_split_first_chunk() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val encryptNonce = arrayOf(1325026333, 711465266)
        val encryptKey: SecretKey =
            SecretKeySpec(byteArrayOf(0x03, 0x1C, 0xBD.toByte(), 0xBA.toByte(), 0x73, 0x42,
                0xFD.toByte(), 0xB0.toByte(), 0x95.toByte(), 0x13, 0x81.toByte(), 0xAB.toByte(),
                0x97.toByte(), 0x94.toByte(), 0x8C.toByte(), 0xD9.toByte()), "AES")
        val plainText =
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02,
                0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05)
        val splitter = FruityDataEncryptAndSplit(encryptNonce, encryptKey)
        val expect =
            byteArrayOf(0x72.toByte(), 0x64.toByte(), 0xA4.toByte(), 0xB6.toByte(), 0xA5.toByte(),
                0x5F.toByte(), 0x8E.toByte(), 0x30.toByte(), 0x6B.toByte(), 0x46.toByte(),
                0x7C.toByte(), 0x00.toByte(), 0xC4.toByte(), 0x24.toByte(), 0x6A.toByte(),
                0xE0.toByte(), 0x8F.toByte(), 0x6E.toByte(), 0x5D.toByte(), 0x4F.toByte())
        assertThat(splitter.chunk(plainText, 0, 23), `is`(expect))
    }

    @Test
    fun test_split_last_chunk() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val encryptNonce = arrayOf(1325026333, 711465266)
        val encryptKey: SecretKey =
            SecretKeySpec(byteArrayOf(0x03, 0x1C, 0xBD.toByte(), 0xBA.toByte(), 0x73, 0x42,
                0xFD.toByte(), 0xB0.toByte(), 0x95.toByte(), 0x13, 0x81.toByte(), 0xAB.toByte(),
                0x97.toByte(), 0x94.toByte(), 0x8C.toByte(), 0xD9.toByte()), "AES")
        val plainText =
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02,
                0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05)
        val expect =
            byteArrayOf(0x47, 0xE7.toByte(), 0x30, 0x36, 0x4B, 0x1F, 0x81.toByte(), 0xA4.toByte(),
                0x3, 0x59, 0x3F, 0x8E.toByte(), 0x92.toByte(), 0xE2.toByte(), 0x47, 0x5,
                0xB6.toByte())
        val splitter = FruityDataEncryptAndSplit(encryptNonce, encryptKey)
        var index = 0
        var target = ByteArray(0)
        var splitData: ByteArray? = ByteArray(0)
        while (splitData != null) {
            splitData = splitter.chunk(plainText, index++, 23)
            if (splitData != null) {
                target = ByteArray(splitData.size)
                System.arraycopy(splitData, 0, target, 0, splitData.size)
            }
        }
        assertThat(target, `is`(expect))
    }

    @Test
    fun test_generate_mic() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val encryptNonce = arrayOf(1325026333, 711465266)
        val encryptKey: SecretKey =
            SecretKeySpec(byteArrayOf(0x03, 0x1C, 0xBD.toByte(), 0xBA.toByte(), 0x73, 0x42,
                0xFD.toByte(), 0xB0.toByte(), 0x95.toByte(), 0x13, 0x81.toByte(), 0xAB.toByte(),
                0x97.toByte(), 0x94.toByte(), 0x8C.toByte(), 0xD9.toByte()), "AES")
        val encryptedPacket =
            byteArrayOf(0x79, 0x65, 0xA5.toByte(), 0xB6.toByte(), 0xA6.toByte(), 0xA7.toByte(),
                0x58, 0x89.toByte(), 0x0D, 0xE8.toByte(), 0x77, 0xED.toByte(), 0xDC.toByte(), 0x26,
                0x69, 0xE4.toByte())
        val expect = byteArrayOf(0xCA.toByte(), 0xCA.toByte(), 0x47.toByte(), 0x57.toByte())
        assertThat(FruityDataEncryptAndSplit(encryptNonce, encryptKey).generateMIC(encryptNonce,
            encryptKey, encryptedPacket, 13), `is`(expect))
    }

    @Test
    fun test_mic_validation() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val encryptNonce = arrayOf(1325026333, 711465266)
        val encryptKey: SecretKey =
            SecretKeySpec(byteArrayOf(0x03, 0x1C, 0xBD.toByte(), 0xBA.toByte(), 0x73, 0x42,
                0xFD.toByte(), 0xB0.toByte(), 0x95.toByte(), 0x13, 0x81.toByte(), 0xAB.toByte(),
                0x97.toByte(), 0x94.toByte(), 0x8C.toByte(), 0xD9.toByte()), "AES")
        val encryptedPacket = byteArrayOf(0x79, 0x65, 0xA5.toByte(), 0xB6.toByte(), 0xA6.toByte(),
            0xA7.toByte(), 0x58.toByte(), 0x89.toByte(), 0x0D.toByte(), 0xE8.toByte(),
            0x77.toByte(), 0xED.toByte(), 0xDC.toByte(), 0xCA.toByte(), 0xCA.toByte(),
            0x47.toByte(), 0x57.toByte()
        )
        val temp: MeshAccessDataCallback =
            object : MeshAccessDataCallback() {
                override fun sendPacket(
                    data: Data, encryptionNonce: Array<Int>?, encryptionKey: SecretKey?,
                ) {
                }

                override fun initialize() {
                }

            }
        assert(temp.checkMicValidation(
            encryptedPacket, encryptNonce, encryptKey))
    }

    @Test
    fun test_decrypt_packet() {
        // first, (byte)0x 0x4EFA4C1D
        // second, (byte)0x 0x2A681932
        val aNonce = arrayOf(1325026333, 711465266)
        val sessionKey: SecretKey = SecretKeySpec(byteArrayOf(0x03, 0x1C, 0xBD.toByte(),
            0xBA.toByte(), 0x73, 0x42, 0xFD.toByte(), 0xB0.toByte(), 0x95.toByte(), 0x13,
            0x81.toByte(), 0xAB.toByte(), 0x97.toByte(), 0x94.toByte(), 0x8C.toByte(),
            0xD9.toByte()), "AES")
        val encryptedPacket = byteArrayOf(0x79.toByte(), 0x65.toByte(), 0xA5.toByte(),
            0xB6.toByte(), 0xA6.toByte(), 0xA7.toByte(), 0x58.toByte(), 0x89.toByte(),
            0x0D.toByte(), 0xE8.toByte(), 0x77.toByte(), 0xED.toByte(),
            0xDC.toByte(), 0xCA.toByte(), 0xCA.toByte(), 0x47.toByte(), 0x57.toByte())
        val expect = byteArrayOf(0x1B.toByte(), 0x01.toByte(), 0x00.toByte(), 0x02.toByte(),
            0x00.toByte(), 0xFC.toByte(), 0xD3.toByte(), 0xB8.toByte(),
            0x64.toByte(), 0xAD.toByte(), 0x0F.toByte(), 0xE8.toByte(), 0x19.toByte())
        val temp: MeshAccessDataCallback =
            object : MeshAccessDataCallback() {
                override fun sendPacket(
                    data: Data, encryptionNonce: Array<Int>?, encryptionKey: SecretKey?,
                ) {
                }

                override fun initialize() {
                }

            }
        assertThat(temp.decryptPacket(encryptedPacket, encryptedPacket.size, aNonce, sessionKey),
            `is`(expect))
    }


}