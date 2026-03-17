package com.reeled.quizoverlay.trigger

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundAppDetector(private val context: Context) {

    companion object {
        val TARGET_PACKAGES = setOf(
            "com.instagram.android",
            "com.instagram.lite",
            "com.zhiliaoapp.musically",         // TikTok
            "com.ss.android.ugc.trill",         // TikTok alternate package
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.pinterest",
        )

        private const val QUERY_WINDOW_MS = 2 * 60 * 1000L // 2 minutes
    }

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    fun getCurrentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - QUERY_WINDOW_MS, now)
            ?: return null

        var latestForegroundPackage: String? = null
        var latestForegroundTime = 0L

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                if (event.timeStamp >= latestForegroundTime) {
                    latestForegroundTime = event.timeStamp
                    latestForegroundPackage = event.packageName
                }
            }
        }

        return latestForegroundPackage
    }

    fun isTargetAppInForeground(monitoredApps: Set<String> = emptySet()): Boolean {
        val current = getCurrentForegroundPackage() ?: return false
        val targets = if (monitoredApps.isNotEmpty()) monitoredApps else TARGET_PACKAGES
        return current in targets
    }
}
