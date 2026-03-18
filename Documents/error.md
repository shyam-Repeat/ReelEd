Debugging Android Overlay Without Android Studio
Here's a systematic approach to debug this effectively on-device:

1. Verify Permission at Runtime (Don't Trust Settings Screen)
Android's Settings.canDrawOverlays() can lie — always check right before drawing:
kotlinfun canShowOverlay(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(this)
    } else true
}

// Log this RIGHT before windowManager.addView()
Log.d("OVERLAY", "Can draw overlays: ${canShowOverlay()}")

2. The Most Common Silent Failure — Wrong Window Type or Flags
kotlinval params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    // ✅ Must be this on API 26+
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    // ✅ These flags are critical
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
    PixelFormat.TRANSLUCENT
)
Silent killer: Using TYPE_PHONE or TYPE_SYSTEM_ALERT on API 26+ causes a silent denial — no crash, no log.

3. Wrap addView in Try-Catch (You're Likely Missing the Exception)
kotlintry {
    windowManager.addView(overlayView, params)
    log("Overlay added successfully") // your dev log
} catch (e: WindowManager.BadTokenException) {
    log("BadTokenException: ${e.message}")
} catch (e: Exception) {
    log("addView failed: ${e.javaClass.simpleName} — ${e.message}")
}
This is the #1 reason "no log comes" — the crash happens but nobody catches it.

4. Use a Foreground Service (YT/Instagram Require This)
YouTube and Instagram are full-screen apps. The overlay must come from a Foreground Service, not an Activity:
kotlinclass OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification()) // Required!
        showOverlay()
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay, null)
        
        val params = WindowManager.LayoutParams(
            300, 300,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
        
        try {
            windowManager.addView(overlayView, params)
            log("✅ Overlay added")
        } catch (e: Exception) {
            log("❌ ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

5. On-Device Logging Without Android Studio
Since your dev screen misses system logs, use this helper to write to a file you can read:
kotlinobject DevLog {
    private val file = File("/sdcard/overlay_debug.txt")
    
    fun log(msg: String) {
        val line = "${System.currentTimeMillis()}: $msg\n"
        file.appendText(line)
        Log.d("OVERLAY_DEBUG", msg) // still log normally
    }
}
Then read /sdcard/overlay_debug.txt with any file manager. This captures everything including caught exceptions.

6. Checklist for YT/Instagram Specifically
CheckWhyService is ForegroundBackground services get killedFOREGROUND_SERVICE permission in manifestCrashes silently otherwiseSYSTEM_ALERT_WINDOW in manifestRequired even with runtime grantView has non-zero size0x0 view renders invisiblyView background is setTransparent view is invisibleTest with a colored TextView firstRules out layout issues

7. Quick Sanity Test
Before anything else, test with the simplest possible overlay:
kotlinval tv = TextView(this).apply {
    text = "OVERLAY TEST"
    setBackgroundColor(Color.RED)
    setTextColor(Color.WHITE)
    textSize = 24f
}
windowManager.addView(tv, params)
If this red box doesn't appear over YouTube, the issue is permission/service, not your layout.

Most likely culprits in order:

addView is throwing an exception you're not catching
Overlay is being called from an Activity context instead of a Foreground Service
View has zero size or is fully transparent
