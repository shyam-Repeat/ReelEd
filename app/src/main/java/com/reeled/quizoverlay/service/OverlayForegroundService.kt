package com.reeled.quizoverlay.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.reeled.quizoverlay.data.model.TriggerConfig
import com.reeled.quizoverlay.data.model.TriggerDecision
import com.reeled.quizoverlay.di.AppContainer
import com.reeled.quizoverlay.trigger.TriggerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var container: AppContainer
    private lateinit var triggerEngine: TriggerEngine

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        triggerEngine = TriggerEngine(container.overlayPrefs, container.quizRepository)
        startForeground(101, Notification.Builder(this).setContentTitle("ReelEd active").build())
        startPollingLoop()
    }

    private fun startPollingLoop() {
        serviceScope.launch {
            while (isActive) {
                when (val decision = triggerEngine.checkAndFire()) {
                    is TriggerDecision.Fire -> {
                        container.overlayPrefs.recordQuizShown(
                            questionId = decision.question.id,
                            dismissed = false,
                            now = System.currentTimeMillis()
                        )
                    }

                    is TriggerDecision.Skip -> Unit
                }
                delay(TriggerConfig.POLLING_INTERVAL_MS)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
