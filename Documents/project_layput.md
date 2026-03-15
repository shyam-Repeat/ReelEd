# PROJECT STRUCTURE DEFINITION
## AI Quiz Overlay — Android MVP
### Version: R1

---

## OVERVIEW

One Android project. One Gradle module. Simple MVVM.
No multi-module setup for MVP — that adds complexity with
zero benefit at 10-tester scale. Every package below is a
folder inside the single app module.

Root package name: com.reeled.quizoverlay
(The package name has been set to com.reeled.quizoverlay. This name cannot change after first build
without breaking Room migrations and DataStore keys.)

---

## TOP-LEVEL PROJECT LAYOUT
```
quizoverlay/
├── .github/
│   └── workflows/
│       └── build.yml           ← GitHub Actions APK build
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/reeled/quizoverlay/
│   │   │   └── res/
│   │   └── test/               ← unit tests (minimal for MVP)
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## PACKAGE STRUCTURE
### Inside java/com/reeled/quizoverlay/

---

### ROOT
```
quizoverlay/
├── App.kt                      ← Application class. Initialises Firebase
│                                 Crashlytics, WorkManager, and Supabase client.
└── MainActivity.kt             ← Single activity. Hosts the Compose nav graph.
                                  Reads entry destination from AppPrefs and
                                  routes to the correct start screen.
```

---

### UI PACKAGE
All screens and reusable Compose components.
No business logic lives here. ViewModels are the boundary.
```
ui/
├── navigation/
│   └── AppNavGraph.kt          ← Single NavHost. All route strings defined here
│                                 as constants. No magic strings anywhere else.
│
├── onboarding/
│   ├── WelcomeScreen.kt
│   ├── ConsentScreen.kt        ← Parental consent (COPPA/GDPR-K)
│   ├── PinSetupScreen.kt       ← Parent creates 4-digit PIN
│   ├── PermissionOverlayScreen.kt   ← SYSTEM_ALERT_WINDOW
│   ├── PermissionUsageScreen.kt     ← PACKAGE_USAGE_STATS
│   ├── PermissionNotifScreen.kt     ← Notification (API 33+)
│   ├── BatteryOptScreen.kt          ← Disable battery optimisation
│   └── OnboardingViewModel.kt
│
├── childhome/
│   ├── ChildHomeScreen.kt      ← What the child sees. Status only.
│   │                             Single "Parent? Tap here" link at bottom.
│   └── ChildHomeViewModel.kt
│
├── dashboard/
│   ├── ParentDashboardScreen.kt
│   ├── TodaySummaryCard.kt
│   ├── WeekBarCard.kt
│   ├── SubjectBreakdownCard.kt
│   ├── RecentAttemptsCard.kt
│   └── DashboardViewModel.kt
│
├── pin/
│   ├── PinGateDialog.kt        ← Reusable PIN entry dialog used by
│   │                             dashboard, disable, and quick pause.
│   └── PinActivity.kt          ← Standalone Activity opened from
│                                 notification action and overlay card
│                                 corner tap. No nav graph connection.
│
├── overlay/
│   ├── QuizCardRouter.kt       ← Reads card_type, dispatches to card
│   ├── cards/
│   │   ├── TapChoiceCard.kt
│   │   ├── TapTapMatchCard.kt
│   │   ├── DragDropMatchCard.kt
│   │   └── FillBlankCard.kt
│   └── components/
│       ├── TimerBar.kt         ← Reusable countdown strip
│       ├── OptionButton.kt     ← Reusable answer button with color states
│       ├── ChipItem.kt         ← Reusable draggable/tappable chip
│       └── ParentCornerButton.kt ← Small lock icon in card corner
│                                    Opens PinActivity for quick pause
│
└── theme/
    ├── Color.kt
    ├── Typography.kt
    ├── Shape.kt
    └── Theme.kt
```

---

### SERVICE PACKAGE
Android Services and the overlay lifecycle owner.
No Compose here except the ComposeView instantiation.
```
service/
├── OverlayForegroundService.kt ← Main service. Hosts the polling loop,
│                                 WindowManager overlay management,
│                                 and result callback wiring.
├── OverlayLifecycleOwner.kt    ← Minimal LifecycleOwner + SavedState-
│                                 RegistryOwner implementation required
│                                 to host ComposeView in a Service.
└── ServiceRestartReceiver.kt   ← BroadcastReceiver that restarts the
                                  Foreground Service if the OS kills it.
                                  Declared in AndroidManifest.
```

---

### TRIGGER PACKAGE
All trigger logic. Pure Kotlin, no Android framework dependencies
except DataStore reads via the prefs wrapper.
```
trigger/
├── TriggerEngine.kt            ← Gate checks + selectNextQuestion().
│                                 Returns TriggerDecision.Fire or Skip.
├── TriggerDecision.kt          ← Sealed class: Fire(question) | Skip(reason)
├── TriggerConfig.kt            ← All timing constants in one place.
│                                 WARMUP_MS, COOLDOWN_MS, MAX_DAILY, etc.
├── ForegroundAppDetector.kt    ← UsageStatsManager wrapper.
│                                 Returns current foreground package name.
│                                 TARGET_PACKAGES list lives here.
└── VideoPlaybackDetector.kt    ← Three-signal heuristic.
                                  Combines foreground check + AudioManager.
```

---

### DATA PACKAGE
Everything that touches storage or network.
Split into three sub-packages: local, remote, repository.
```
data/
│
├── local/
│   ├── AppDatabase.kt          ← Room database class. Lists all DAOs.
│   │                             Version starts at 1. Bump + write
│   │                             migration class for every schema change.
│   ├── dao/
│   │   ├── QuizQuestionDao.kt
│   │   ├── QuizAttemptDao.kt
│   │   └── EventLogDao.kt
│   └── entity/
│       ├── QuizQuestionEntity.kt
│       ├── QuizAttemptEntity.kt
│       └── EventLogEntity.kt
│
├── remote/
│   ├── SupabaseClient.kt       ← Retrofit instance. Base URL, OkHttp client,
│   │                             timeouts, and Crashlytics error interceptor.
│   ├── SupabaseApi.kt          ← Retrofit interface. Two endpoints only:
│   │                             GET quiz_questions (fetch active questions)
│   │                             POST quiz_attempts + event_logs (batch sync)
│   └── dto/
│       ├── QuizQuestionDto.kt  ← JSON shape matching Supabase columns.
│       ├── QuizAttemptDto.kt     toEntity() and toDto() extension functions
│       └── EventLogDto.kt        live on these classes, not on entities.
│
└── repository/
    └── QuizRepository.kt       ← Single repository. All data access for
                                  the app goes through here. ViewModels and
                                  the TriggerEngine depend on this class only —
                                  never directly on DAOs or the API interface.
```

---

### MODEL PACKAGE
Pure Kotlin data classes. No Android, Room, or Retrofit dependencies.
These are the domain objects passed between layers.
```
model/
├── QuizCardConfig.kt           ← Master config object. Parsed from entity.
├── QuizPayload.kt              ← Sealed class: TapChoicePayload,
│                                 TapTapMatchPayload, DragDropPayload,
│                                 FillBlankPayload
├── QuizDisplay.kt
├── QuizRules.kt
├── QuizAttemptResult.kt        ← Unified result from all card types
├── QuizCardType.kt             ← Enum: TAP_CHOICE | TAP_TAP_MATCH |
│                                 DRAG_DROP_MATCH | FILL_BLANK
└── payload/
    ├── ChoiceOption.kt
    ├── MatchPair.kt
    ├── DragChip.kt
    ├── DropSlot.kt
    └── WordChip.kt
```

---

### WORKER PACKAGE
WorkManager workers only. Each worker does one job and nothing else.
```
worker/
├── QuizFetchWorker.kt          ← Checks Room question count.
│                                 Fetches from Supabase if below minimum.
│                                 Runs on app open + once daily.
├── SyncWorker.kt               ← Reads unsynced attempts and event logs
│                                 from Room. Batch posts to Supabase.
│                                 Marks rows synced on success.
└── ServiceWatchdogWorker.kt    ← Checks if Foreground Service is running.
                                  Restarts it if not and overlay is enabled.
                                  Runs every 15 minutes.
```

---

### PREFS PACKAGE
DataStore wrappers. One class per concern.
No raw DataStore calls outside this package.
```
prefs/
├── AppPrefs.kt                 ← Onboarding state, PIN hash, consent flag,
│                                 overlay enabled flag, tester_id.
│                                 Used by MainActivity for entry routing.
├── TriggerPrefs.kt             ← Session start time, last quiz shown time,
│                                 quizzes shown today, last question id,
│                                 last was dismissed flag, parent pause
│                                 active flag and expiry timestamp.
│                                 Used exclusively by TriggerEngine.
└── PinPrefs.kt                 ← PIN hash, failed attempt count,
                                  lockout expiry timestamp.
                                  Used exclusively by pin package.
```

---

### UTIL PACKAGE
Stateless helpers. No state, no dependencies on other packages.
```
util/
├── PinHasher.kt                ← SHA-256 hash and verify for PIN.
├── UuidGenerator.kt            ← Generates tester_id on first install.
├── TimeUtils.kt                ← Epoch ms helpers, midnight reset calc,
│                                 week start calculation for dashboard.
├── AudioMuter.kt               ← Mute and restore system media stream.
│                                 Used by OverlayForegroundService.
└── NotificationBuilder.kt      ← Builds the persistent Foreground Service
                                  notification with both states:
                                  active and paused. Action buttons wired
                                  to PinActivity via PendingIntent.
```

---

## RESOURCE STRUCTURE
### Inside app/src/main/res/
```
res/
├── drawable/
│   ├── ic_brain.xml            ← App icon / notification small icon
│   └── ic_lock.xml             ← Parent corner button icon on quiz card
├── values/
│   ├── strings.xml             ← All user-facing text. No hard-coded
│   │                             strings in Kotlin or Compose files.
│   ├── colors.xml              ← Base palette only. Theme colours
│   │                             defined in ui/theme/Color.kt
│   └── dimens.xml              ← Card corner radius, padding constants,
                                  minimum tap target sizes (44dp rule)
```

---

## ANDROIDMANIFEST DECLARATIONS SUMMARY

Permissions declared:
  SYSTEM_ALERT_WINDOW
  PACKAGE_USAGE_STATS
  FOREGROUND_SERVICE
  FOREGROUND_SERVICE_SPECIAL_USE (API 34+)
  POST_NOTIFICATIONS (API 33+)
  REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
  INTERNET
  RECEIVE_BOOT_COMPLETED          ← restarts service after phone reboot

Components declared:
  MainActivity                    ← singleTask launch mode
  PinActivity                     ← no history, no animation
  OverlayForegroundService        ← foreground service type: specialUse
  ServiceRestartReceiver          ← exported false, internal only

---

## GITHUB ACTIONS — BUILD PIPELINE
### .github/workflows/build.yml

Single workflow. Triggers on every push to main branch.

Steps in order:
  1. Checkout code
  2. Set up JDK 17
  3. Cache Gradle dependencies
  4. Run ./gradlew assembleDebug
  5. Upload APK as build artifact

The APK is available for download from the Actions run page.
Send the download link to your 10 testers directly.
No Play Store, no Firebase App Distribution for MVP.

Secrets required in GitHub repository settings:
  None for debug build.
  Add KEYSTORE and signing secrets only when moving to release build.

---

## DEPENDENCY RESPONSIBILITY MAP

Who depends on what. If a package is not listed as a dependency
of another package, it has no business importing from it.

  ui           → model, prefs (AppPrefs only)
  service      → model, data/repository, prefs, trigger, util
  trigger      → model, data/repository, prefs/TriggerPrefs
  data/local   → model (entity only)
  data/remote  → model (dto only)
  data/repository → data/local, data/remote, model
  worker       → data/repository, prefs, service (start intent only)
  prefs        → util/PinHasher, util/UuidGenerator
  util         → nothing (no internal dependencies)
  model        → nothing (no internal dependencies)

Violations of this map are architecture bugs.
The most common violation to watch for: ui importing from
data/local directly, bypassing the repository. Never allowed.

---

## BUILD ORDER FOR MVP

Build files and packages in this sequence. Each step is
independently testable before the next begins.

  Step 1 — Foundation
    model package (all data classes)
    util package (all helpers)
    prefs package (DataStore wrappers)

  Step 2 — Storage
    data/local (Room entities, DAOs, database)
    data/remote (Retrofit client, API interface, DTOs)
    data/repository (QuizRepository)

  Step 3 — Detection and Trigger
    trigger package (ForegroundAppDetector, VideoPlaybackDetector,
    TriggerConfig, TriggerEngine)

  Step 4 — Service and Overlay
    service package (OverlayLifecycleOwner, OverlayForegroundService,
    ServiceRestartReceiver)
    ui/overlay (QuizCardRouter and all 4 card Composables)

  Step 5 — Workers
    worker package (QuizFetchWorker, SyncWorker, ServiceWatchdogWorker)

  Step 6 — App Shell
    ui/theme
    ui/pin (PinActivity, PinGateDialog)
    ui/onboarding (all screens)
    ui/childhome
    ui/dashboard
    ui/navigation (AppNavGraph)
    MainActivity

  Step 7 — Wiring
    App.kt (initialise all singletons)
    AndroidManifest.xml (all declarations)
    GitHub Actions workflow

---

*End of Project Structure Definition — R1*
*Follow the build order. Do not skip to Step 6 UI before*
*Step 3 trigger logic is verified working on a real device.*