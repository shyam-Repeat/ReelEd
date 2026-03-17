package com.reeled.quizoverlay.trigger

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundAppDetector(private val context: Context) {

    companion object {
        val TARGET_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",         // TikTok
            "com.ss.android.ugc.trill",         // TikTok alternate package
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.pinterest",
        )

        private const val QUERY_WINDOW_MS = 5_000L
    }

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    fun getCurrentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - QUERY_WINDOW_MS, now)
            ?: return null

        var latestPackage: String? = null
        var latestTime = 0L

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.timeStamp > latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }
        return latestPackage
    }

    fun isTargetAppInForeground(): Boolean {
        return getCurrentForegroundPackage() in TARGET_PACKAGES
    }
}
