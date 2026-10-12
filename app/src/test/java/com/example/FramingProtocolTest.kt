package com.example

import com.example.protocol.ProvisioningProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FramingProtocolTest {

    private lateinit var protocol: ProvisioningProtocol

    @Before
    fun setUp() {
        protocol = ProvisioningProtocol()
    }

    @Test
    fun testSingleFrameEncodingAndDecoding() {
        val sampleJson = """{"cmd":"wifi_status","reqId":"req_101"}"""
        val frames = protocol.encodeMessage(sampleJson, maxFrameSize = 128)

        assertEquals(1, frames.size)

        val frame = frames[0]
        assertEquals(ProvisioningProtocol.PROTOCOL_VERSION, frame[0])

        val result = protocol.decodeFrame(frame)
        assertTrue(result is ProvisioningProtocol.ReassemblyResult.Complete)
        val complete = result as ProvisioningProtocol.ReassemblyResult.Complete
        assertEquals(sampleJson, complete.json)
    }

    @Test
    fun testMultiChunkEncodingAndReassembly() {
        // Create a long JSON payload that will span multiple chunks
        val longJson = """{"cmd":"wifi_connect","ssid":"LongEnterpriseNetwork_5GHz_Primary","password":"VerySecureSuperLongPasswordWithSpecialSymbols!@#$%^&*()","reqId":"req_chunk_001"}"""

        // Force a small maxFrameSize of 30 bytes (5 byte header + 25 byte payload)
        val frames = protocol.encodeMessage(longJson, maxFrameSize = 30)
        assertTrue(frames.size > 1)

        var finalResult: ProvisioningProtocol.ReassemblyResult? = null
        for (frame in frames) {
            finalResult = protocol.decodeFrame(frame)
        }

        assertTrue(finalResult is ProvisioningProtocol.ReassemblyResult.Complete)
        val complete = finalResult as ProvisioningProtocol.ReassemblyResult.Complete
        assertEquals(longJson, complete.json)
    }

    @Test
    fun testRejectionOfInvalidProtocolVersion() {
        val badFrame = byteArrayOf(0x99.toByte(), 0xC1.toByte(), 0x01, 0x00, 0x01, 0x7B, 0x7D)
        val result = protocol.decodeFrame(badFrame)
        assertTrue(result is ProvisioningProtocol.ReassemblyResult.Error)
        val error = result as ProvisioningProtocol.ReassemblyResult.Error
        assertTrue(error.reason.contains("Unsupported protocol version"))
    }

    @Test
    fun testRejectionOfOutOfOrderChunks() {
        val sampleJson = """{"cmd":"wifi_connect","ssid":"TestSSID","password":"SecretPassword123"}"""
        val frames = protocol.encodeMessage(sampleJson, maxFrameSize = 25)
        assertTrue(frames.size >= 2)

        // Decode first chunk (index 0)
        val res1 = protocol.decodeFrame(frames[0])
        assertTrue(res1 is ProvisioningProtocol.ReassemblyResult.Partial)

        // Intentionally skip to chunk 2 (if exists) or replay chunk 0
        val resOutOrder = protocol.decodeFrame(frames[0])
        assertTrue(resOutOrder is ProvisioningProtocol.ReassemblyResult.Error)
    }
}
