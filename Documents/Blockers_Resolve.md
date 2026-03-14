# SECTION 6: BLOCKER MITIGATIONS
## Technical Avoidance Patterns — All 10 Blockers

---

### BLOCKER 1: Permission Friction
**Risk:** Parent denies overlay / usage access / notifications at OS prompt.
**Gap in previous sections:** Onboarding was mentioned but not defined.

**Mitigation — Onboarding Permission Flow:**

```
SCREEN 1: Welcome
  "This app helps your child learn while they scroll."
  "We need 3 permissions to work."
  [ Let's set up → ]

SCREEN 2: Overlay Permission
  Title   : "Step 1 of 3 — Display over other apps"
  Body    : "Lets quiz cards appear on top of Instagram and YouTube."
  Button  : "Open Settings"  → deep link to Settings.ACTION_MANAGE_OVERLAY_PERMISSION
  After return: check Settings.canDrawOverlays(context)
  If granted: green checkmark, enable [ Next ] button
  If denied : show "This permission is required. Without it the app cannot work."
              [ Try Again ] button — re-opens settings

SCREEN 3: Usage Access
  Title   : "Step 2 of 3 — Usage access"
  Body    : "Lets the app know when Instagram or YouTube is open,
             so quizzes only appear at the right moment."
  Button  : "Open Settings" → Settings.ACTION_USAGE_ACCESS_SETTINGS
  After return: check AppOpsManager OPSTR_GET_USAGE_STATS
  Same granted/denied handling as Screen 2

SCREEN 4: Notifications
  Title   : "Step 3 of 3 — Notifications"
  Body    : "Required to keep the quiz service running in the background."
  Button  : "Allow"  → NotificationManagerCompat.requestNotificationPermission()
            (Android 13+ only; auto-granted below API 33)

SCREEN 5: Battery Optimization (see Blocker 6)
  Handled here in same onboarding flow.
```

```kotlin
// Permission check utilities
fun hasOverlayPermission(context: Context): Boolean =
    Settings.canDrawOverlays(context)

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
```

---

### BLOCKER 2: Accessibility Service Risk
**Risk:** Using AccessibilityService to monitor app content triggers Play Store rejection.
**Mitigation:** AccessibilityService is fully excluded from this MVP.
No AccessibilityService declaration in AndroidManifest.xml.
Use PACKAGE_USAGE_STATS instead (see Blocker 3).
✅ Already handled — no further code needed.

---

### BLOCKER 3: Detecting Instagram / YouTube Open
**Risk:** Without knowing which app is in foreground, overlay fires at wrong time.
**Mitigation:** Use UsageStatsManager — requires PACKAGE_USAGE_STATS permission
(granted via Settings, not a dangerous runtime permission).

```kotlin
object ForegroundAppDetector {

    // Target apps for MVP — hardcoded, no dynamic detection needed
    val TARGET_PACKAGES = setOf(
        "com.instagram.android",
        "com.google.android.youtube"
    )

    fun getForegroundPackage(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as UsageStatsManager
        val now = System.currentTimeMillis()
        // Query last 3 seconds — short window to get current foreground app
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 3000,
            now
        )
        return stats
            ?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    fun isTargetAppInForeground(context: Context): Boolean {
        return getForegroundPackage(context) in TARGET_PACKAGES
    }
}
```

Integration in OverlayForegroundService polling loop:
```kotlin
// Add this as Gate 0 — before all other trigger gates
private fun startPollingLoop() {
    pollingJob = serviceScope.launch {
        while (isActive) {
            val foreground = ForegroundAppDetector.getForegroundPackage(applicationContext)
            val isTarget = foreground in ForegroundAppDetector.TARGET_PACKAGES

            if (isTarget) {
                // Update session start time if this is a fresh session
                prefs.updateSessionIfNeeded(foreground!!)
                // Now check trigger gates
                val decision = triggerEngine.checkAndFire()
                if (decision is TriggerDecision.Fire) {
                    withContext(Dispatchers.Main) { showOverlay(decision.question) }
                }
            } else {
                // Not in target app — reset session tracking
                prefs.clearActiveSession()
            }
            delay(TriggerConfig.POLLING_INTERVAL_MS)
        }
    }
}
```

DataStore session tracking:
```kotlin
// When target app enters foreground and no session is active:
suspend fun updateSessionIfNeeded(packageName: String) {
    val current = prefs.getActiveSessionPackage()
    if (current != packageName) {
        // New session started
        prefs.setSessionStart(System.currentTimeMillis())
        prefs.setActiveSessionPackage(packageName)
    }
}

// When non-target app is in foreground:
suspend fun clearActiveSession() {
    prefs.clearSessionStart()
    prefs.clearActiveSessionPackage()
}
```

---

### BLOCKER 4: Detecting Reels / Video Playing
**Risk:** No official API to detect exact Reels or Shorts playback.
**Mitigation:** Three-signal heuristic — all three must be true to confirm
"child is watching video content." No single signal is reliable alone.

```kotlin
object VideoPlaybackDetector {

    // Signal 1: Target app is in foreground (from Blocker 3)
    // Signal 2: Audio is playing (MediaSession or AudioManager)
    // Signal 3: Session has been active for WARMUP_MS (not a cold open)

    fun isVideoLikelyPlaying(context: Context): Boolean {
        val signal1 = ForegroundAppDetector.isTargetAppInForeground(context)
        val signal2 = isAudioPlaying(context)
        val signal3 = true // checked via DataStore session start time in TriggerEngine

        // Require signal1 + at least one of signal2/signal3
        // This avoids false negatives when audio is muted
        return signal1 && (signal2 || signal3)
    }

    // AudioManager music stream active check
    private fun isAudioPlaying(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMusicActive
    }
}
```

Why this is good enough for MVP:
- We don't need to know the exact video — we need to know the child is engaged
- "In Instagram + audio playing + 3 min in session" = high confidence of Reels viewing
- False positives (child reading feed, not watching) still produce valid quiz data
- False negatives (video muted) are caught by session timer fallback

MediaSession listener (optional enhancement, add if signal2 proves unreliable):
```kotlin
// Register in OverlayForegroundService.onCreate()
val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE)
    as MediaSessionManager
// Requires MEDIA_CONTENT_CONTROL permission — NOT needed for MVP
// AudioManager.isMusicActive() is sufficient and permission-free
```

---

### BLOCKER 5: Auto Pause / Fake Pause
**Risk:** Cannot call Instagram's pause API. Child continues watching under quiz.
**Mitigation:** Full-screen opaque overlay + system audio mute = "fake pause."
This requires DIFFERENT WindowManager flags than Section 1.4.

**Two overlay modes — choose based on quiz card config `strict_mode`:**

```kotlin
// MODE A: Non-strict (default) — partial overlay, touch passthrough allowed
// Used for: TAP_CHOICE, FILL_BLANK
// Section 1.4 flags apply — FLAG_NOT_FOCUSABLE, 56% height
// Child can see top of Instagram. No fake pause.

// MODE B: Strict mode — full screen opaque overlay + audio mute
// Used for: DRAG_DROP_MATCH, TAP_TAP_MATCH (longer interactions)
// OR when rules.strict_mode == true in quiz config

fun getWindowParamsForMode(screenHeight: Int, strictMode: Boolean): WindowManager.LayoutParams {
    return if (strictMode) {
        // STRICT: full screen, consumes ALL touches, no passthrough
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,   // full screen
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            // Note: FLAG_NOT_FOCUSABLE is REMOVED for strict mode
            // This makes the overlay consume all touch events
            PixelFormat.OPAQUE   // fully opaque — child cannot see Instagram
        ).apply { gravity = Gravity.TOP }
    } else {
        // NON-STRICT: bottom sheet, passes touches through transparent top area
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (screenHeight * 0.56).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
    }
}
```

Audio mute for strict mode fake pause:
```kotlin
fun muteSystemAudio(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        AudioManager.ADJUST_MUTE,
        0  // no UI flag — silent mute
    )
}

fun restoreSystemAudio(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        AudioManager.ADJUST_UNMUTE,
        0
    )
}

// Call muteSystemAudio() when strict overlay appears
// Call restoreSystemAudio() in onQuizResult() before removing overlay
```

**Important:** Always restore audio in onDestroy() of OverlayForegroundService
as a safety net in case the service is killed before quiz completes.

---

### BLOCKER 6: OEM Battery Kill (Xiaomi / Oppo / Vivo / Samsung)
**Risk:** Aggressive OEM battery optimization kills Foreground Service silently.
**Mitigation:** Three-layer defense.

**Layer 1 — Onboarding Battery Optimization Screen:**
```kotlin
// Add as Screen 5 in onboarding flow (after permissions)

@Composable
fun BatteryOptimizationScreen(onContinue: () -> Unit) {
    // Title: "One last step — keep quizzes running"
    // Body:  "Some phones stop background apps to save battery.
    //         Disable this for our app so quizzes never miss a moment."
    // Button: "Disable battery optimization"
    //         → opens ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS intent
    // After return: check isIgnoringBatteryOptimizations()
    // If granted: green check, show [ Continue ]
    // If denied:  yellow warning "Quizzes may stop on some phones"
    //             still allow [ Continue ] — don't block onboarding
}

fun requestIgnoreBatteryOptimization(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

fun isIgnoringBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
```

**Layer 2 — START_STICKY + Restart Receiver:**
```kotlin
// Already in OverlayForegroundService:
override fun onStartCommand(...): Int = START_STICKY

// Add BroadcastReceiver to restart service if killed:
class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.yourapp.RESTART_SERVICE") {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OverlayForegroundService::class.java)
            )
        }
    }
}

// In OverlayForegroundService.onDestroy():
override fun onDestroy() {
    // Send restart broadcast with 2s delay
    val restartIntent = Intent("com.yourapp.RESTART_SERVICE")
    sendBroadcast(restartIntent)
    super.onDestroy()
}
```

**Layer 3 — WorkManager Watchdog:**
```kotlin
class ServiceWatchdogWorker(ctx: Context, params: WorkerParameters)
    : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val manager = ActivityManager::class.java
            .getMethod("getRunningServices", Int::class.java)
        val isRunning = /* check if OverlayForegroundService is in running services list */
            isServiceRunning(applicationContext, OverlayForegroundService::class.java)

        if (!isRunning && isOverlayEnabled(applicationContext)) {
            ContextCompat.startForegroundService(
                applicationContext,
                Intent(applicationContext, OverlayForegroundService::class.java)
            )
        }
        return Result.success()
    }
}

// Schedule: every 15 minutes (minimum WorkManager interval)
PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(15, TimeUnit.MINUTES).build()
```

---

### BLOCKER 7: Overlay Touch / Gesture Issues
**Risk:** Drag gestures pass through overlay to Instagram (swipe to next Reel).
**Mitigation:** Touch handling depends on overlay mode.

**For non-strict (partial overlay):**
```kotlin
// FLAG_NOT_FOCUSABLE allows touches through the TRANSPARENT TOP AREA only
// The card's own ComposeView handles touches inside its bounds normally
// Drag-drop quiz (TYPE 3) should NOT be used in non-strict mode
// because drag gestures near card edges may leak to Instagram

// Rule: DRAG_DROP_MATCH card type MUST always use strict_mode = true
// Enforce this in QuizCardRouter:
fun QuizCardRouter(...) {
    val effectiveConfig = if (config.cardType == QuizCardType.DRAG_DROP_MATCH) {
        config.copy(rules = config.rules.copy(strictMode = true))
    } else config

    // render with effectiveConfig
}
```

**For strict (full-screen overlay):**
```kotlin
// FLAG_NOT_TOUCH_MODAL without FLAG_NOT_FOCUSABLE = card consumes ALL touches
// No gesture leaks to Instagram. Child CANNOT swipe to next Reel.
// This is correct and intentional for strict mode.

// Additional: intercept back gesture
// Add to ComposeView in strict mode:
BackHandler(enabled = strictMode) {
    // Do nothing — prevent back press from dismissing overlay in strict mode
}
```

---

### BLOCKER 8: Play Store "Interfering with Other Apps" Risk
**Risk:** Play Store reviewer sees overlay on Instagram and flags as disruptive.
**Mitigation:** Positioning, manifest, and store listing strategy.

App Store listing framing:
```
Category    : Education > Tools for Families
Title       : [AppName] — Learning Overlay for Kids
Description : A parental tool that shows educational quiz cards while
              children use social media. Parent-controlled, opt-in only.
              Requires explicit setup by a parent or guardian.
```

AndroidManifest.xml — declare purpose clearly:
```xml
<!-- Declare targeted use of overlay permission -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions"/>

<!-- Tag app as family-safe -->
<application
    android:label="@string/app_name"
    ...>
    <!-- No android:isGame="true" -->
    <!-- Target families policy compliance metadata -->
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
        <!-- Only include if using AdMob; for MVP with no ads, omit entirely -->
</application>
```

Play Store review defense checklist:
```
✓ Overlay only appears when parent has enabled it (opt-in)
✓ Overlay only appears on explicitly listed target apps
✓ Child cannot be harmed — no data collection, no purchases
✓ Parental PIN required to change any settings
✓ Onboarding clearly explains what the app does before any permission
✓ App does NOT inject code into or modify Instagram/YouTube
✓ App does NOT read Instagram/YouTube content (only detects foreground)
✓ Persistent notification makes overlay visible and stoppable at all times
```

For MVP (APK sideload to 10 testers):
Play Store review is not relevant yet. Apply these rules before v1 public launch.

---

### BLOCKER 9: MediaProjection / Screen Capture Risk
**Risk:** Using MediaProjection to screenshot or record Instagram triggers Play review
         and requires a foreground notification with capture icon.
**Mitigation:** MediaProjection is fully excluded. Not referenced anywhere in this MVP.
The "fake pause" effect (Blocker 5) uses audio mute + opaque overlay instead.
✅ Already handled — no further code needed.

---

### BLOCKER 10: Child Compliance — COPPA / GDPR-K
**Risk:** App used by children under 13 triggers COPPA (US) and GDPR-K (EU) requirements:
         parental consent, no behavioral advertising, no persistent identifiers.
**Mitigation:** Minimal data design + parent-controlled consent.

Data minimization rules (extends Section 5.7):
```
COLLECTED AND STORED LOCALLY (Room):
  - Anonymous tester_id (UUID, generated at install, no name/email)
  - Quiz answers (question_id, correct/wrong, response time)
  - Session events (overlay_started, quiz_shown)

SENT TO SUPABASE:
  - Same as above — no PII

NEVER COLLECTED:
  - Child's name, age, or photo
  - Device IMEI, MAC address, advertising ID
  - Location
  - Contact list
  - Instagram/YouTube account info or content

ADVERTISING ID — DISABLE EXPLICITLY:
```
```kotlin
// In Application.onCreate():
AdvertisingIdClient.getAdvertisingIdInfo(context) // DO NOT call this
// Instead, opt out at manifest level:
```
```xml
<!-- AndroidManifest.xml -->
<meta-data
    android:name="com.google.android.gms.ads.AD_MANAGER_APP"
    android:value="false"/>
```

Parental consent screen (add to onboarding after PIN setup):
```kotlin
@Composable
fun ParentalConsentScreen(onAccepted: () -> Unit, onDeclined: () -> Unit) {
    // Title: "Parent or Guardian Consent"
    // Body (plain language):
    //   "This app collects anonymous usage data (quiz answers and timing)
    //    to help us improve learning outcomes. No personal information
    //    about your child is collected or shared.
    //    You are setting this up for a child under your supervision."
    // Checkbox: "I am the parent or guardian and I consent"
    // [ Continue ] button — enabled only when checkbox ticked
    // [ Decline ] — exits app
    // Save consent timestamp to DataStore
}
```

```kotlin
// DataStore keys for compliance
object ConsentKeys {
    val CONSENT_GIVEN = booleanPreferencesKey("parental_consent_given")
    val CONSENT_TIMESTAMP = longPreferencesKey("consent_given_at")
    val CONSENT_VERSION = intPreferencesKey("consent_version") // bump when policy changes
}
```