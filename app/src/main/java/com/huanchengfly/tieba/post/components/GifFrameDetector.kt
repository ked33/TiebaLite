package com.huanchengfly.tieba.post.components

import java.io.InputStream

/**
 * Reads GIF block metadata, never pixels. Null means malformed, truncated or beyond the scan limit;
 * callers can use a bounded bitmap probe instead of assuming an AnimatedImageDrawable.
 */
internal fun hasMultipleGifFrames(input: InputStream, maxScanBytes: Int = 8 * 1024 * 1024): Boolean? {
    val reader = GifBlockReader(input, maxScanBytes)
    val signature = reader.readBytes(6) ?: return null
    if (!signature.contentEquals("GIF87a".toByteArray(Charsets.US_ASCII)) &&
        !signature.contentEquals("GIF89a".toByteArray(Charsets.US_ASCII))
    ) return null

    val screenDescriptor = reader.readBytes(7) ?: return null
    if (!reader.skipColorTable(screenDescriptor[4].toInt() and 0xff)) return null

    var frames = 0
    while (true) {
        when (reader.readByte()) {
            0x2c -> {
                val imageDescriptor = reader.readBytes(9) ?: return null
                if (!reader.skipColorTable(imageDescriptor[8].toInt() and 0xff)) return null
                if (reader.readByte() !in 2..8) return null // LZW minimum code size.
                if (!reader.skipSubBlocks()) return null
                frames++
                if (frames >= 2) return true
            }

            0x21 -> {
                if (reader.readByte() < 0 || !reader.skipSubBlocks()) return null
            }

            0x3b -> return if (frames == 1) false else null
            else -> return null
        }
    }
}

private class GifBlockReader(private val input: InputStream, private var remaining: Int) {
    fun readByte(): Int {
        if (remaining <= 0) return -1
        remaining--
        return input.read()
    }

    fun readBytes(count: Int): ByteArray? {
        val bytes = ByteArray(count)
        for (index in bytes.indices) {
            val value = readByte()
            if (value < 0) return null
            bytes[index] = value.toByte()
        }
        return bytes
    }

    fun skipColorTable(packedFields: Int): Boolean =
        packedFields and 0x80 == 0 || skipBytes(3 * (2 shl (packedFields and 7)))

    fun skipSubBlocks(): Boolean {
        while (true) {
            val size = readByte()
            if (size < 0) return false
            if (size == 0) return true
            if (!skipBytes(size)) return false
        }
    }

    private fun skipBytes(count: Int): Boolean {
        if (count > remaining) return false
        var pending = count
        while (pending > 0) {
            val skipped = input.skip(pending.toLong()).toInt()
            if (skipped > 0) {
                remaining -= skipped
                pending -= skipped
            } else {
                if (readByte() < 0) return false
                pending--
            }
        }
        return true
    }
}
