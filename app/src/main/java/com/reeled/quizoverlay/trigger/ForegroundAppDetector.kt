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
    }

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private var lastKnownForegroundPackage: String? = null
    private var lastQueryTime: Long = 0L

    fun getCurrentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        
        // Look back 24 hours on first run to prime the cache,
        // otherwise overlap by a small amount to catch recent events.
        val queryStart = if (lastQueryTime == 0L) {
            now - (24 * 60 * 60 * 1000L) 
        } else {
            lastQueryTime - 1000L
        }

        val events = usageStatsManager.queryEvents(queryStart, now)
        var latestForegroundTime = 0L

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == 1 // Fallback for ACTIVITY_RESUMED on older APIs
                ) {
                    if (event.timeStamp >= latestForegroundTime) {
                        latestForegroundTime = event.timeStamp
                        lastKnownForegroundPackage = event.packageName
                    }
                }
            }
        }

        lastQueryTime = now
        return lastKnownForegroundPackage
    }

    fun isTargetAppInForeground(monitoredApps: Set<String> = emptySet()): Boolean {
        val current = getCurrentForegroundPackage() ?: return false
        val targets = if (monitoredApps.isNotEmpty()) monitoredApps else TARGET_PACKAGES
        return current in targets
    }
}
