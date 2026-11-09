package com.huanchengfly.tieba.post.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.AssetDataSource
import com.github.panpf.sketch.datasource.BasedFileDataSource
import com.github.panpf.sketch.datasource.BasedStreamDataSource
import com.github.panpf.sketch.datasource.ByteArrayDataSource
import com.github.panpf.sketch.datasource.ContentDataSource
import com.github.panpf.sketch.datasource.ResourceDataSource
import com.github.panpf.sketch.decode.DrawableDecoder
import com.github.panpf.sketch.decode.HeifAnimatedDrawableDecoder
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.internal.RequestContext
import java.io.IOException
import java.nio.ByteBuffer

/** Sketch's animated decoders reject single-frame GIF/WebP/HEIF containers. */
@RequiresApi(Build.VERSION_CODES.P)
class SingleFrameSafeAnimatedDecoderFactory(
    private val delegate: DrawableDecoder.Factory,
    private val checkGifFrames: Boolean = false,
) : DrawableDecoder.Factory {
    override fun create(
        sketch: Sketch,
        requestContext: RequestContext,
        fetchResult: FetchResult,
    ): DrawableDecoder? {
        // Let Sketch reject other formats, unsupported sources and disallowed animations first.
        val decoder = delegate.create(sketch, requestContext, fetchResult) ?: return null
        val multipleFrames = try {
            val dataSource = fetchResult.dataSource
            val gifFrames = if (checkGifFrames && dataSource is BasedStreamDataSource) {
                dataSource.newInputStream().buffered().use { hasMultipleGifFrames(it) }
            } else null
            // An unknown/large GIF still gets the sampled platform decoder when animated.
            gifFrames ?: probeAnimation(sketch, fetchResult)
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        return decoder.takeIf { multipleFrames == true }
    }

    private fun probeAnimation(sketch: Sketch, fetchResult: FetchResult): Boolean? {
        val source = when (val dataSource = fetchResult.dataSource) {
            is AssetDataSource -> ImageDecoder.createSource(sketch.context.assets, dataSource.assetFileName)
            is ResourceDataSource -> ImageDecoder.createSource(dataSource.resources, dataSource.resId)
            is ContentDataSource -> ImageDecoder.createSource(sketch.context.contentResolver, dataSource.contentUri)
            is ByteArrayDataSource -> ImageDecoder.createSource(ByteBuffer.wrap(dataSource.data))
            is BasedFileDataSource -> ImageDecoder.createSource(dataSource.getFile())
            else -> return null
        }
        var animated = false
        val listener = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
            animated = info.isAnimated
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSize(1, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            delegate is HeifAnimatedDrawableDecoder.Factory
        ) {
            // HEIF can contain both a still image and an animation. decodeBitmap prefers the still
            // image, so use the same selection policy as Sketch's actual drawable decoder here.
            val drawable = ImageDecoder.decodeDrawable(source, listener)
            return try {
                animated
            } finally {
                when (drawable) {
                    is AnimatedImageDrawable -> {
                        drawable.stop()
                        drawable.clearAnimationCallbacks()
                    }
                    is BitmapDrawable -> drawable.bitmap.recycle()
                }
                drawable.callback = null
            }
        }
        // GIF/WebP need only a first-frame bitmap; release its pixels immediately. The actual
        // request still uses Sketch's target dimensions, including large GIFs beyond the scan cap.
        val bitmap = ImageDecoder.decodeBitmap(source, listener)
        return try {
            animated
        } finally {
            bitmap.recycle()
        }
    }
}
