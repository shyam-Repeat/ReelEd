# TODO LIST - AI Quiz Overlay MVP (R1)

This list tracks the pending work required to complete the MVP based on `Documents/MVP_R01.md`, `Documents/tech.md`, and `Documents/project_layput.md`.

---

## 🟥 HIGH PRIORITY: DATA & GLUE LAYER
*Status: Most files exist as stubs/empty.*

- [ ] **Data - Local (Room)**
    - [ ] Complete `data/local/AppDatabase.kt` definition.
    - [ ] Implement `QuizQuestionDao`, `QuizAttemptDao`, and `EventLogDao` with required queries (especially for Dashboard).
- [ ] **Data - Remote (Supabase)**
    - [ ] Complete `data/remote/SupabaseClient.kt` (OkHttp + Retrofit setup).
    - [ ] Implement `data/remote/SupabaseApi.kt` endpoints (GET questions, POST attempts/logs).
- [ ] **Repository**
    - [ ] Implement `data/repository/QuizRepository.kt` as the single source of truth for UI and Service.
- [ ] **App Infrastructure**
    - [ ] Implement `App.kt`: Initialize Room, Supabase, WorkManager, and Crashlytics.
    - [ ] Implement `MainActivity.kt`: Setup `NavHost` and entry routing logic (Onboarding -> Home).

---

## 🟧 UI & ONBOARDING
*Status: Screens exist but need logic verification.*

- [ ] **Onboarding Flow (8 Steps)**
    - [ ] Verify/Complete `WelcomeScreen`.
    - [ ] Verify/Complete `ConsentScreen` (Parental consent logic).
    - [ ] Verify/Complete `PinSetupScreen` (SHA-256 hashing via `PinHasher`).
    - [ ] Verify/Complete `PermissionOverlayScreen` (`SYSTEM_ALERT_WINDOW`).
    - [ ] Verify/Complete `PermissionUsageScreen` (`PACKAGE_USAGE_STATS`).
    - [ ] Verify/Complete `PermissionNotifScreen` (API 33+).
    - [ ] Verify/Complete `BatteryOptScreen` (Disable optimization).
- [ ] **Parent Dashboard**
    - [x] Implement `ParentDashboardScreen` cards: `TodaySummary`, `WeekBar`, `SubjectBreakdown`, and `RecentAttempts`.
    - [ ] Connect `DashboardViewModel` to `QuizRepository` (currently using `sampleUiState`).
    - [ ] Implement `showPinPrompt` and `openFeedback` logic in `DashboardViewModel`.
- [ ] **Overlay UI**
    - [ ] Ensure `QuizCardRouter` correctly handles mode switching (Strict vs Non-Strict).
    - [ ] Verify `TimerBar` and `OptionButton` state management.

---

## 🟨 BACKGROUND TASKS (WORKERS)
*Status: Missing implementation.*

- [ ] **QuizFetchWorker**: Fetch questions from Supabase if Room count < 30.
- [ ] **SyncWorker**: Batch sync unsynced attempts and logs to Supabase every 30 mins.
- [ ] **ServiceWatchdogWorker**: Ensure `OverlayForegroundService` is restarted if killed.

---

## 🟦 SERVICE & INTEGRATION
*Status: Service partially started.*

- [ ] **OverlayForegroundService**
    - [ ] Connect to `QuizRepository` to write results.
    - [ ] Connect to `TriggerEngine` (Note: Trigger logic is being handled by user).
    - [ ] Implement Audio Muting/Restoring for "Strict Mode".
- [ ] **Manifest & Permissions**
    - [ ] Verify all permissions and service types in `AndroidManifest.xml`.
    - [ ] Ensure `RECEIVE_BOOT_COMPLETED` is wired to restart the service.

---

## ⬜️ REFINEMENT
- [ ] Move all hardcoded strings to `res/values/strings.xml`.
- [ ] Verify dependency map: UI should only talk to `Repository` or `AppPrefs`.
- [x] Setup GitHub Actions build pipeline in `.github/workflows/build.yml`.
- [x] Initialize Gradle build system (root & app `build.gradle.kts`, `settings.gradle.kts`).
- [x] Generate Gradle wrapper (`gradlew`).
- [x] Initialize basic resources and App/MainActivity stubs for build stability.

---
*Generated on: March 15, 2026*
