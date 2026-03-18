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
        
        // On first run, look back up to 2 hours to prime the current state.
        // On subsequent runs, just overlap slightly with the last query.
        val queryStart = if (lastQueryTime == 0L) {
            now - (2 * 60 * 60 * 1000L) 
        } else {
            lastQueryTime - 2000L
        }

        val events = usageStatsManager.queryEvents(queryStart, now)

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, 
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        lastKnownForegroundPackage = event.packageName
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, 
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        // If the current foreground app is being paused/backgrounded, clear the cache.
                        // (Usually followed immediately by another app's RESUMED event which will overwrite this null)
                        if (lastKnownForegroundPackage == event.packageName) {
                            lastKnownForegroundPackage = null
                        }
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
