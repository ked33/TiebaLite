package com.huanchengfly.tieba.post.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.MsgBean
import com.huanchengfly.tieba.post.pendingIntentFlagImmutable
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotifyJobService : JobService() {
    private var notificationManager: NotificationManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeRequest: NotificationRequest? = null

    private class NotificationRequest(val params: JobParameters) {
        var call: Call<MsgBean>? = null
    }

    private fun createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                id,
                name, NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.group = CHANNEL_GROUP
            channel.setShowBadge(true)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Log.i(TAG, "onStartJob")
        activeRequest?.let { previous ->
            previous.call?.cancel()
            finishRequest(previous, false)
        }
        val request = NotificationRequest(params)
        activeRequest = request
        try {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelGroup = NotificationChannelGroup(CHANNEL_GROUP, CHANNEL_GROUP_NAME)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    channelGroup.description = "贴吧的各种消息通知"
                }
                notificationManager?.createNotificationChannelGroup(channelGroup)
                createChannel(CHANNEL_REPLY, CHANNEL_REPLY_NAME)
                createChannel(CHANNEL_AT, CHANNEL_AT_NAME)
            }
            val call = TiebaApi.getInstance().msg()
            request.call = call
            call.enqueue(object : Callback<MsgBean> {
                override fun onFailure(call: Call<MsgBean>, t: Throwable) {
                    mainHandler.post { finishRequest(request, true) }
                }

                override fun onResponse(call: Call<MsgBean>, response: Response<MsgBean>) {
                    mainHandler.post {
                        if (activeRequest !== request) return@post
                        var needsReschedule = true
                        try {
                            val message = response.body()?.message
                            if (response.isSuccessful && message != null) {
                                publishMessages(message)
                                needsReschedule = false
                            }
                        } catch (error: Exception) {
                            Log.w(TAG, "Unable to publish notification results", error)
                        } finally {
                            finishRequest(request, needsReschedule)
                        }
                    }
                }
            })
        } catch (error: Exception) {
            Log.w(TAG, "Unable to start notification check", error)
            request.call?.cancel()
            mainHandler.post { finishRequest(request, true) }
        }
        return true
    }

    private fun finishRequest(request: NotificationRequest, needsReschedule: Boolean) {
        if (activeRequest !== request) return
        activeRequest = null
        request.call = null
        jobFinished(request.params, needsReschedule)
    }

    private fun cancelRequest() {
        val request = activeRequest ?: return
        // Invalidate first: cancel() may deliver an asynchronous failure callback.
        activeRequest = null
        request.call?.cancel()
        request.call = null
    }

    override fun onStopJob(params: JobParameters): Boolean {
        if (activeRequest?.params?.jobId == params.jobId) {
            cancelRequest()
        }
        return true
    }

    override fun onDestroy() {
        cancelRequest()
        mainHandler.removeCallbacksAndMessages(null)
        notificationManager = null
        super.onDestroy()
    }

    private fun publishMessages(message: MsgBean.MessageBean) {
        val replyCount = message.replyMe?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val atCount = message.atMe?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        if (replyCount > 0) {
            sendMessageCount(CHANNEL_REPLY, replyCount)
            updateNotification(
                getString(R.string.tips_message_reply, message.replyMe),
                ID_REPLY,
                CHANNEL_REPLY,
                CHANNEL_REPLY_NAME,
                Intent(ACTION_VIEW, Uri.parse("tblite://notifications/0"))
            )
        }
        if (atCount > 0) {
            sendMessageCount(CHANNEL_AT, atCount)
            updateNotification(
                getString(R.string.tips_message_at, message.atMe),
                ID_AT,
                CHANNEL_AT,
                CHANNEL_AT_NAME,
                Intent(ACTION_VIEW, Uri.parse("tblite://notifications/1"))
            )
        }
        val total = (replyCount.toLong() + atCount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        sendMessageCount(CHANNEL_TOTAL, total)
    }

    private fun sendMessageCount(channel: String, count: Int) {
        sendBroadcast(
            Intent(ACTION_NEW_MESSAGE)
                .setPackage(packageName)
                .putExtra("channel", channel)
                .putExtra("count", count)
        )
    }

    private fun updateNotification(
        text: String,
        id: Int,
        channel: String,
        channelName: String,
        intent: Intent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(this, channel)
            .setSubText(channelName)
            .setContentText(getString(R.string.tip_touch_to_view))
            .setContentTitle(text)
            .setSmallIcon(R.drawable.ic_round_drafts)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    pendingIntentFlagImmutable()
                )
            )
            .setColor(ThemeUtils.getColorByAttr(this, R.attr.colorPrimary))
            .build()
        notificationManager?.notify(id, notification)
    }

    companion object {
        val TAG = NotifyJobService::class.java.simpleName
        const val ACTION_NEW_MESSAGE = "com.huanchengfly.tieba.post.action.NEW_MESSAGE"
        const val CHANNEL_GROUP = "20"
        const val CHANNEL_AT = "3"
        const val CHANNEL_AT_NAME = "提到我的"
        const val CHANNEL_TOTAL = "total"
        const val ID_REPLY = 20
        const val ID_AT = 21
        private const val CHANNEL_GROUP_NAME = "消息通知"
        private const val CHANNEL_REPLY = "2"
        private const val CHANNEL_REPLY_NAME = "回复我的"
    }
}