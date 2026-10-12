package com.example.protocol

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Message Framing Protocol (Version 0x01)
 *
 * Designed for reliable transmission of arbitrary UTF-8 JSON payloads over BLE GATT
 * characteristics, regardless of the negotiated MTU size.
 *
 * Frame Format (Binary Header + UTF-8 JSON Payload):
 * +----------------+---------------+----------------+----------------+----------------+------------------------+
 * | Byte 0: Ver    | Byte 1: Flags | Byte 2: MsgSeq | Byte 3: ChkIdx | Byte 4: TotChk | Bytes 5..N: Payload    |
 * +----------------+---------------+----------------+----------------+----------------+------------------------+
 *
 * - Byte 0 (Version): 0x01
 * - Byte 1 (Flags):
 *     Bit 7: START_FLAG (0x80) - First chunk of message
 *     Bit 6: END_FLAG   (0x40) - Last chunk of message
 *     Bit 5..0: Message Type (0x01 = JSON Payload, 0x02 = Ping, 0x03 = Status)
 * - Byte 2 (Message Sequence ID): Rolling 0..255 ID identifying the multi-chunk message
 * - Byte 3 (Chunk Index): 0-indexed position within the message (0, 1, ..., TotChk - 1)
 * - Byte 4 (Total Chunks): Total number of chunks in this message (1..255)
 * - Bytes 5..N: Raw chunk payload
 */
class ProvisioningProtocol {

    companion object {
        const val PROTOCOL_VERSION: Byte = 0x01
        const val HEADER_SIZE = 5

        const val FLAG_START: Byte = 0x80.toByte()
        const val FLAG_END: Byte = 0x40.toByte()
        const val MSG_TYPE_JSON: Byte = 0x01

        const val MAX_MESSAGE_SIZE = 65536 // 64 KB safety limit
        const val REASSEMBLY_TIMEOUT_MS = 5000L // 5 seconds chunk timeout
    }

    private var nextSeqId: Int = 0

    // In-progress reassembly state
    private var activeSeqId: Int? = null
    private var expectedTotalChunks: Int = 0
    private var lastReceivedChunkIndex: Int = -1
    private val chunkBuffer = ByteArrayOutputStream()
    private var lastChunkTimestamp: Long = 0

    sealed class ReassemblyResult {
        data class Complete(val json: String, val seqId: Int) : ReassemblyResult()
        data class Partial(val receivedChunks: Int, val totalChunks: Int, val seqId: Int) : ReassemblyResult()
        data class Error(val reason: String) : ReassemblyResult()
    }

    /**
     * Splits a JSON string into framed BLE chunks according to the maximum frame size.
     *
     * @param json Payload to transmit.
     * @param maxFrameSize Total maximum bytes per GATT write (e.g. negotiated MTU - 3).
     * @return Ordered list of framed ByteArrays to write to the BLE characteristic.
     */
    @Synchronized
    fun encodeMessage(json: String, maxFrameSize: Int): List<ByteArray> {
        val payloadBytes = json.toByteArray(StandardCharsets.UTF_8)
        val maxPayloadPerChunk = (maxFrameSize - HEADER_SIZE).coerceAtLeast(1)

        val totalChunks = ((payloadBytes.size + maxPayloadPerChunk - 1) / maxPayloadPerChunk).coerceAtLeast(1)
        require(totalChunks <= 255) { "Payload requires $totalChunks chunks which exceeds protocol maximum of 255" }

        val seqId = (nextSeqId++) and 0xFF
        val frames = mutableListOf<ByteArray>()

        for (chunkIdx in 0 until totalChunks) {
            val offset = chunkIdx * maxPayloadPerChunk
            val length = (payloadBytes.size - offset).coerceAtMost(maxPayloadPerChunk)

            val isStart = chunkIdx == 0
            val isEnd = chunkIdx == totalChunks - 1

            var flags = MSG_TYPE_JSON.toInt()
            if (isStart) flags = flags or FLAG_START.toInt()
            if (isEnd) flags = flags or FLAG_END.toInt()

            val frame = ByteArray(HEADER_SIZE + length)
            frame[0] = PROTOCOL_VERSION
            frame[1] = flags.toByte()
            frame[2] = seqId.toByte()
            frame[3] = chunkIdx.toByte()
            frame[4] = totalChunks.toByte()

            System.arraycopy(payloadBytes, offset, frame, HEADER_SIZE, length)
            frames.add(frame)
        }

        return frames
    }

    /**
     * Ingests a received raw BLE notification/indication frame and attempts reassembly.
     *
     * @param frame Raw bytes from onCharacteristicChanged.
     * @return ReassemblyResult indicating whether the full JSON message has been reassembled.
     */
    @Synchronized
    fun decodeFrame(frame: ByteArray): ReassemblyResult {
        val now = System.currentTimeMillis()

        // Check if an active reassembly expired
        if (activeSeqId != null && (now - lastChunkTimestamp > REASSEMBLY_TIMEOUT_MS)) {
            resetReassembly()
            return ReassemblyResult.Error("Multi-chunk reassembly timed out (> ${REASSEMBLY_TIMEOUT_MS}ms)")
        }

        if (frame.size < HEADER_SIZE) {
            return ReassemblyResult.Error("Malformed frame: size ${frame.size} is less than header size $HEADER_SIZE")
        }

        val version = frame[0]
        if (version != PROTOCOL_VERSION) {
            return ReassemblyResult.Error("Unsupported protocol version: $version (expected $PROTOCOL_VERSION)")
        }

        val flags = frame[1].toInt()
        val isStart = (flags and FLAG_START.toInt()) != 0
        val isEnd = (flags and FLAG_END.toInt()) != 0
        val seqId = frame[2].toInt() and 0xFF
        val chunkIdx = frame[3].toInt() and 0xFF
        val totalChunks = frame[4].toInt() and 0xFF

        if (totalChunks == 0 || chunkIdx >= totalChunks) {
            return ReassemblyResult.Error("Invalid chunk index $chunkIdx for total chunks $totalChunks")
        }

        // Validate sequence continuity
        if (isStart) {
            // New message start
            activeSeqId = seqId
            expectedTotalChunks = totalChunks
            lastReceivedChunkIndex = 0
            chunkBuffer.reset()
            lastChunkTimestamp = now
        } else {
            // Continuation chunk
            if (activeSeqId == null || activeSeqId != seqId) {
                return ReassemblyResult.Error("Received continuation chunk for seqId $seqId without matching start")
            }
            if (chunkIdx != lastReceivedChunkIndex + 1) {
                val expected = lastReceivedChunkIndex + 1
                resetReassembly()
                return ReassemblyResult.Error("Out of order chunk: received $chunkIdx, expected $expected")
            }
            lastReceivedChunkIndex = chunkIdx
            lastChunkTimestamp = now
        }

        val payloadLength = frame.size - HEADER_SIZE
        if (chunkBuffer.size() + payloadLength > MAX_MESSAGE_SIZE) {
            resetReassembly()
            return ReassemblyResult.Error("Message payload exceeded maximum size limit ($MAX_MESSAGE_SIZE bytes)")
        }

        chunkBuffer.write(frame, HEADER_SIZE, payloadLength)

        return if (isEnd) {
            val completeBytes = chunkBuffer.toByteArray()
            resetReassembly()
            try {
                val jsonStr = String(completeBytes, StandardCharsets.UTF_8)
                ReassemblyResult.Complete(jsonStr, seqId)
            } catch (e: Exception) {
                ReassemblyResult.Error("UTF-8 decoding error: ${e.message}")
            }
        } else {
            ReassemblyResult.Partial(chunkIdx + 1, totalChunks, seqId)
        }
    }

    @Synchronized
    fun resetReassembly() {
        activeSeqId = null
        expectedTotalChunks = 0
        lastReceivedChunkIndex = -1
        chunkBuffer.reset()
        lastChunkTimestamp = 0
    }
}
