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

        private const val QUERY_WINDOW_MS = 60 * 60 * 1000L // 1 hour
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
        var latestType = -1

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestPackage = event.packageName
                latestType = event.eventType
            }
        }

        // If the latest event for the package was RESUMED, it's still in foreground.
        // If it was PAUSED or STOPPED, then the app is no longer in foreground.
        return if (latestType == UsageEvents.Event.ACTIVITY_RESUMED) {
            latestPackage
        } else {
            null
        }
    }

    fun isTargetAppInForeground(monitoredApps: Set<String> = emptySet()): Boolean {
        val current = getCurrentForegroundPackage() ?: return false
        val targets = if (monitoredApps.isNotEmpty()) monitoredApps else TARGET_PACKAGES
        return current in targets
    }
}
