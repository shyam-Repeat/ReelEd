package com.reeled.quizoverlay.trigger

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.reeled.quizoverlay.service.ReelEdNotificationListenerService
import com.reeled.quizoverlay.util.PermissionChecker

class MediaSessionAppDetector(private val context: Context) {

    data class Snapshot(
        val notificationListenerGranted: Boolean,
        val sessionCount: Int,
        val detectedPackage: String?,
        val activePackages: List<String>,
        val playingPackages: List<String>
    )

    private val mediaSessionManager by lazy {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    fun getCurrentPlayingPackage(monitoredApps: Set<String> = emptySet()): String? {
        return getSnapshot(monitoredApps).detectedPackage
    }

    fun getSnapshot(monitoredApps: Set<String> = emptySet()): Snapshot {
        val hasAccess = PermissionChecker.hasNotificationListenerAccess(context)
        if (!hasAccess) {
            return Snapshot(
                notificationListenerGranted = false,
                sessionCount = 0,
                detectedPackage = null,
                activePackages = emptyList(),
                playingPackages = emptyList()
            )
        }

        val targets = if (monitoredApps.isNotEmpty()) monitoredApps else ForegroundAppDetector.TARGET_PACKAGES
        val listener = ComponentName(context, ReelEdNotificationListenerService::class.java)

        val sessions = try {
            mediaSessionManager.getActiveSessions(listener)
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val activePackages = sessions.map { it.packageName }.distinct()
        val playingPackages = sessions
            .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            .map { it.packageName }
            .distinct()

        return Snapshot(
            notificationListenerGranted = true,
            sessionCount = sessions.size,
            detectedPackage = selectBestSessionPackage(sessions, targets),
            activePackages = activePackages,
            playingPackages = playingPackages
        )
    }

    private fun selectBestSessionPackage(
        sessions: List<MediaController>,
        targets: Set<String>
    ): String? {
        val playingTarget = sessions.firstOrNull { controller ->
            controller.packageName in targets && controller.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        if (playingTarget != null) return playingTarget.packageName

        val playingAny = sessions.firstOrNull { controller ->
            controller.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        if (playingAny != null) return playingAny.packageName

        val pausedTarget = sessions.firstOrNull { controller ->
            controller.packageName in targets
        }
        return pausedTarget?.packageName
    }
}
