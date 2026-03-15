package com.reeled.quizoverlay.trigger
package com.yourappname.quizoverlay.trigger

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

// ─────────────────────────────────────────────────────────────────────────────
// ForegroundAppDetector
//
// Responsibilities (layout doc):
//   UsageStatsManager wrapper.
//   Returns the current foreground package name.
//   TARGET_PACKAGES list lives here.
//
// PACKAGE_USAGE_STATS permission must be granted by the user via
// Settings → Special app access → Usage access (handled in onboarding).
//
// Improvement over baseline:
//   Uses queryEvents() instead of queryUsageStats() for accuracy.
//   queryUsageStats() aggregates over time windows and can lag by minutes.
//   queryEvents() reads the live event stream — always shows the most recent
//   MOVE_TO_FOREGROUND event, which is the true current foreground app.
//
// Dependency map (layout doc): trigger → (no internal package deps — only Android SDK)
// ─────────────────────────────────────────────────────────────────────────────

class ForegroundAppDetector(private val context: Context) {

    companion object {
        // ── Target packages ───────────────────────────────────────────────────
        // Quiz overlay fires only when one of these apps is in the foreground.
        // Add/remove package names here as MVP scope evolves.
        // Note: This list is intentionally hardcoded for MVP. A future version
        // could fetch an updatable list from Supabase to avoid an app update.
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

        // Rolling window for event query — 5 seconds is enough to capture the
        // most recent MOVE_TO_FOREGROUND event without reading stale history.
        private const val QUERY_WINDOW_MS = 5_000L
    }

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the package name of the current foreground app, or null if
     * the permission has not been granted or no foreground app can be determined.
     */
    fun getCurrentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - QUERY_WINDOW_MS, now)
            ?: return null

        // Walk the event stream in reverse to find the most recent foreground event
        var latestPackage: String? = null
        var latestTime = 0L

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }

        return latestPackage
    }

    /**
     * Returns true if a target app (social media / video) is currently
     * in the foreground. TriggerEngine calls this as part of Gate checks.
     */
    fun isTargetAppInForeground(): Boolean {
        val pkg = getCurrentForegroundPackage() ?: return false
        return pkg in TARGET_PACKAGES
    }

    /**
     * Returns the foreground package if it is a target app, null otherwise.
     * Used by OverlayForegroundService to log sourceApp on quiz attempts.
     */
    fun getForegroundTargetPackage(): String? {
        val pkg = getCurrentForegroundPackage() ?: return null
        return if (pkg in TARGET_PACKAGES) pkg else null
    }
}