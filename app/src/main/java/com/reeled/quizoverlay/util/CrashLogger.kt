package com.reeled.quizoverlay.util

import android.content.Context
import android.util.Log
import com.reeled.quizoverlay.data.repository.QuizRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.PrintWriter
import java.io.StringWriter

object CrashLogger {
    private const val TAG = "CrashLogger"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val repository = QuizRepository(context)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 1. Log to logcat
            Log.e(TAG, "FATAL EXCEPTION: ${thread.name}", throwable)

            // 2. Save to Room (Blocking because the process is about to die)
            runBlocking {
                try {
                    val sw = StringWriter()
                    throwable.printStackTrace(PrintWriter(sw))
                    val stackTrace = sw.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")

                    repository.logEvent(
                        eventType = "app_crash",
                        payloadJson = "{\"thread\":\"${thread.name}\",\"message\":\"${jsonSafe(throwable.message.orEmpty())}\",\"stacktrace\":\"$stackTrace\"}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to log crash to database", e)
                }
            }

            // 3. Let the system handle the crash (shows "App has stopped" dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun jsonSafe(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
