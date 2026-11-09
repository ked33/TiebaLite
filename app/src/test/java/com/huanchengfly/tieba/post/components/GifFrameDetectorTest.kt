package com.huanchengfly.tieba.post.components

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GifFrameDetectorTest {
    // A complete 1x1 GIF with a global black/white palette and valid LZW image data.
    private val header = bytes("47 49 46 38 39 61 01 00 01 00 80 00 00 00 00 00 ff ff ff")
    private val frame = bytes("2c 00 00 00 00 01 00 01 00 00 02 02 44 01 00")
    private val trailer = bytes("3b")

    @Test
    fun singleFrameGifUsesStaticFallback() {
        assertFalse(detect(header + frame + trailer)!!)
    }

    @Test
    fun animatedGifUsesAnimatedDecoder() {
        assertTrue(detect(header + frame + frame + trailer)!!)
    }

    @Test
    fun extensionPayloadsAreNotMistakenForImageFrames() {
        val comment = bytes("21 fe 03 2c 2c 2c 00")
        val graphicControl = bytes("21 f9 04 00 0a 00 00 00")
        val loopExtension = bytes("21 ff 0b 4e 45 54 53 43 41 50 45 32 2e 30 03 01 00 00 00")
        assertFalse(detect(header + loopExtension + comment + graphicControl + frame + trailer)!!)
        assertTrue(detect(header + graphicControl + frame + comment + graphicControl + frame + trailer)!!)
    }

    @Test
    fun localColorTableAndGif87aAreSupported() {
        val header87 = "GIF87a".toByteArray(Charsets.US_ASCII) + header.drop(6).toByteArray()
        val localFrame = bytes("2c 00 00 00 00 01 00 01 00 80 ff 00 00 00 ff 00 02 02 44 01 00")
        assertFalse(detect(header87 + localFrame + trailer)!!)
        assertTrue(detect(header87 + localFrame + localFrame + trailer)!!)
    }

    @Test
    fun truncatedAndNonGifInputsFallBack() {
        val single = header + frame + trailer
        for (length in 0 until single.size) {
            assertNull("Truncated at $length", detect(single.copyOf(length)))
        }
        assertNull(detect("not a GIF image".toByteArray()))
        assertNull(detect(header + trailer))
    }

    @Test
    fun scanBudgetStopsBeforeReadingTheRestOfTheFile() {
        val content = header + frame + frame + trailer
        val input = ByteArrayInputStream(content)
        assertNull(hasMultipleGifFrames(input, maxScanBytes = 20))
        assertEquals(content.size - 20, input.available())
    }

    @Test
    fun streamsWithoutSkipSupportStillWork() {
        val input = object : ByteArrayInputStream(header + frame + frame + trailer) {
            override fun skip(n: Long): Long = 0
        }
        assertTrue(hasMultipleGifFrames(input)!!)
    }

    private fun detect(bytes: ByteArray): Boolean? = hasMultipleGifFrames(ByteArrayInputStream(bytes))

    private fun bytes(hex: String): ByteArray =
        hex.split(' ').map { it.toInt(16).toByte() }.toByteArray()
}
