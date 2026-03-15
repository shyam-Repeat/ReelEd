# TODO LIST - AI Quiz Overlay MVP (R1)

This list tracks the pending work required to complete the MVP based on `Documents/MVP_R01.md`, `Documents/tech.md`, and `Documents/project_layput.md`.

---

## 🟥 HIGH PRIORITY: DATA & GLUE LAYER
*Status: Room and Supabase infrastructure implemented.*

- [x] **Data - Local (Room)**
    - [x] Complete `data/local/AppDatabase.kt` definition.
    - [x] Implement `QuizQuestionDao`, `QuizAttemptDao`, and `EventLogDao` with required queries.
    - [x] Add Room annotations (`@Entity`, `@PrimaryKey`, etc.) to classes in `data/local/entity/`.
- [x] **Data - Remote (Supabase)**
    - [x] Complete `data/remote/SupabaseClient.kt`.
    - [x] Implement `data/remote/SupabaseApi.kt` endpoints.
- [x] **Repository**
    - [x] Implement `data/repository/QuizRepository.kt` as the single source of truth.
- [x] **App Infrastructure**
    - [x] Implement `App.kt`: Initialize background tasks.
    - [x] Implement `MainActivity.kt`: Setup `NavHost` and basic entry routing.

---

## 🟧 UI & ONBOARDING
*Status: NavGraph implemented; Most screens still stubs.*

- [ ] **Onboarding Flow (8 Steps)**
    - [ ] Verify/Complete `WelcomeScreen` (Stub).
    - [ ] Verify/Complete `ConsentScreen` (Stub).
    - [ ] Verify/Complete `PinSetupScreen` (Partial implementation exists).
    - [ ] Verify/Complete `PermissionOverlayScreen` (Stub).
    - [ ] Verify/Complete `PermissionUsageScreen` (Stub).
    - [ ] Verify/Complete `PermissionNotifScreen` (Stub).
    - [ ] Verify/Complete `BatteryOptScreen` (Stub).
- [ ] **Parent Dashboard**
    - [x] Implement `ParentDashboardScreen` cards: `TodaySummary`, `WeekBar`, `SubjectBreakdown`, and `RecentAttempts`.
    - [ ] Connect `DashboardViewModel` to `QuizRepository`.
    - [ ] Implement `showPinPrompt` and `openFeedback` logic in `DashboardViewModel`.
- [ ] **Overlay UI**
    - [x] `QuizCardRouter` implemented with mode switching (Strict vs Non-Strict).
    - [ ] Verify `TimerBar` and `OptionButton` state management.

---

## 🟨 BACKGROUND TASKS (WORKERS)
*Status: Implementations fixed and connected to Repository.*

- [x] **QuizFetchWorker**: Fetch questions from Supabase if Room count < 30.
- [x] **SyncWorker**: Batch sync unsynced attempts and logs to Supabase every 30 mins.
- [x] **ServiceWatchdogWorker**: Ensure `OverlayForegroundService` is restarted if killed.

---

## 🟦 SERVICE & INTEGRATION
*Status: Service implementation fixed and connected.*

- [x] **OverlayForegroundService**
    - [x] Fix package name and imports.
    - [x] Connect to `QuizRepository` to write results.
    - [x] Connect to `TriggerEngine`.
    - [ ] Implement Audio Muting/Restoring for "Strict Mode" (Muter stub needs implementation).
- [x] **Manifest & Permissions**
    - [x] Verify all permissions and service types in `AndroidManifest.xml`.
    - [x] Ensure `RECEIVE_BOOT_COMPLETED` is wired to restart the service.

---

## ⬜️ REFINEMENT
- [ ] Move all hardcoded strings to `res/values/strings.xml`.
- [ ] Verify dependency map: UI should only talk to `Repository` or `AppPrefs`.
- [x] Setup GitHub Actions build pipeline in `.github/workflows/build.yml`.
- [x] Initialize Gradle build system.
- [x] Generate Gradle wrapper (`gradlew`).
- [x] Initialize basic resources and App/MainActivity.

---
*Updated on: March 15, 2026*
