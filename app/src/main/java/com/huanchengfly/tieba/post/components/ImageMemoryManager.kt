package com.huanchengfly.tieba.post.components

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.internal.LruBitmapPool
import com.github.panpf.sketch.cache.internal.LruMemoryCache
import java.lang.ref.WeakReference
import kotlin.math.roundToLong

/** Bounds decoded-image retention without creating an image loader just to trim it. */
object ImageMemoryManager : Application.ActivityLifecycleCallbacks {
    private const val MIB = 1024L * 1024L
    private const val BACKGROUND_TRIM_DELAY_MILLIS = 30_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var installed = false
    private var startedActivities = 0

    @Volatile
    private var sketchReference = WeakReference<Sketch>(null)

    private val trimBackgroundImages = Runnable {
        if (startedActivities == 0) {
            sketchReference.get()?.let { sketch ->
                sketch.memoryCache.clear()
                sketch.bitmapPool.clear()
            }
        }
    }

    fun install(application: Application) {
        if (!installed) {
            application.registerActivityLifecycleCallbacks(this)
            installed = true
        }
    }

    fun configure(builder: Sketch.Builder, context: Context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val lowRam = activityManager.isLowRamDevice
        val largeHeap = context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
        val memoryClass = if (largeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        val metrics = context.resources.displayMetrics

        // Keep Sketch 3.3.2's six-screen/heap cap on smaller devices as well.
        val defaultBudget = minOf(
            metrics.widthPixels.toLong() * metrics.heightPixels * 4L * 6L,
            (memoryClass * MIB * if (lowRam) 0.25f else 0.33f).roundToLong(),
        )
        val cacheLimit = minOf((if (lowRam) 12L else 24L) * MIB, (defaultBudget * 0.66f).roundToLong())
        val poolLimit = minOf((if (lowRam) 4L else 8L) * MIB, (defaultBudget * 0.33f).roundToLong())
        builder.memoryCache(LruMemoryCache(cacheLimit.coerceAtLeast(1L)))
        builder.bitmapPool(LruBitmapPool(poolLimit.coerceAtLeast(1L)))
    }

    fun onSketchCreated(sketch: Sketch) {
        sketchReference = WeakReference(sketch)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        mainHandler.removeCallbacks(trimBackgroundImages)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0 && !activity.isChangingConfigurations) {
            mainHandler.removeCallbacks(trimBackgroundImages)
            mainHandler.postDelayed(trimBackgroundImages, BACKGROUND_TRIM_DELAY_MILLIS)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
