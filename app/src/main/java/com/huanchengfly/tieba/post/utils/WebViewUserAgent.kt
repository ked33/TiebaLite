package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.webkit.WebView
import androidx.annotation.MainThread

/** Reuses an observed WebView UA without loading its provider for native API requests. */
object WebViewUserAgent {
    private const val PREFERENCES_NAME = "webview_user_agent"
    private const val KEY_USER_AGENT = "default_user_agent"

    @Volatile
    var current: String? = null
        private set

    @MainThread
    fun init(context: Context) {
        if (current == null) {
            current = context.applicationContext
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_AGENT, null)
                ?.takeIf { it.isNotBlank() }
        }
    }

    /** Call immediately after creating a real WebView, before custom UA settings. */
    @MainThread
    fun updateFrom(webView: WebView) {
        val userAgent = webView.settings.userAgentString?.takeIf { it.isNotBlank() } ?: return
        if (current == userAgent) return
        current = userAgent
        webView.context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_AGENT, userAgent)
            .apply()
    }
}
