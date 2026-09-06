package com.huanchengfly.tieba.post.api.models.protos

import org.junit.Assert.assertEquals
import org.junit.Test

class PbContentImageDimensionsTest {
    @Test
    fun validBsizeKeepsItsDimensions() {
        val content = PbContent(bsize = " 640 , 480 ", width = 800, height = 600)

        assertEquals(640 to 480, content.imageDimensions())
    }

    @Test
    fun malformedBsizeFallsBackToProtobufDimensions() {
        listOf("", "640", "wide,480", "640,", "640,480,1", "2147483648,480").forEach { bsize ->
            val content = PbContent(bsize = bsize, width = 800, height = 600)

            assertEquals("bsize=$bsize", 800 to 600, content.imageDimensions())
        }
    }

    @Test
    fun nonPositiveBsizeFallsBackToProtobufDimensions() {
        listOf("0,480", "640,0", "-1,480", "640,-1", "0,0").forEach { bsize ->
            val content = PbContent(bsize = bsize, width = 800, height = 600)

            assertEquals("bsize=$bsize", 800 to 600, content.imageDimensions())
        }
    }

    @Test
    fun missingOrInvalidDimensionsUseSquarePlaceholder() {
        listOf(0 to 0, 800 to 0, 0 to 600, -1 to 600, 800 to -1).forEach { (width, height) ->
            val content = PbContent(bsize = "invalid", width = width, height = height)

            assertEquals("width=$width height=$height", 1 to 1, content.imageDimensions())
        }
    }
}
