# TODO LIST - AI Quiz Overlay MVP (R1)

This checklist was re-audited against the current codebase and docs (`Documents/MVP_R01.md`, `Documents/tech.md`, `Documents/project_layput.md`, and implementation under `app/src/main`).

---

## ✅ COMPLETED FOUNDATIONS
*Status: Core architecture and most MVP plumbing are implemented.*

- [x] **Data - Local (Room)**
    - [x] `AppDatabase` configured.
    - [x] `QuizQuestionDao`, `QuizAttemptDao`, and `EventLogDao` implemented.
    - [x] Room entities and mappers implemented.
- [x] **Data - Remote (Supabase)**
    - [x] `SupabaseClient` configured.
    - [x] `SupabaseApi` endpoints implemented.
- [x] **Repository**
    - [x] `QuizRepository` implemented and used by workers/service.
- [x] **App Infrastructure**
    - [x] `App.kt` initializes WorkManager jobs.
    - [x] `MainActivity.kt` sets up entry routing.
- [x] **Background Tasks (Workers)**
    - [x] `QuizFetchWorker` fetches and caches questions.
    - [x] `SyncWorker` syncs attempts/events.
    - [x] `ServiceWatchdogWorker` + restart receiver path wired.
- [x] **Service Baseline**
    - [x] `OverlayForegroundService` shows overlay and records attempts.
    - [x] Notification channel + foreground lifecycle present.

---

## 🟧 PENDING / INCOMPLETE LOGIC

### 1) Onboarding & Navigation glue
- [ ] Persist PIN in onboarding flow (`AppNavGraph` still has "Save PIN logic would go here" placeholder).
- [ ] Wire real settings intents for:
    - [ ] Overlay permission open-settings action.
    - [ ] Usage access grant action.
    - [ ] Notification permission action.
    - [ ] Battery optimization exemption action.
- [ ] Implement `OnboardingViewModel` (currently empty file).

### 2) Parent Dashboard integration
- [ ] Connect `DashboardViewModel` to `QuizRepository` (currently sample/static state).
- [ ] Implement real `showPinPrompt` flow.
- [ ] Implement real `openFeedback` flow.

### 3) Overlay strict-mode behavior gaps
- [ ] Enforce true strict-mode overlay interaction behavior (strict flag is passed, but window flags are currently not differentiated).
- [ ] Apply audio muting only for strict sessions (currently muting happens whenever overlay is shown).
- [ ] Ensure audio restore is guaranteed on all overlay/service teardown paths.
- [ ] Implement pause/resume notification action handling in service command routing.

### 4) UI quality + platform hardening
- [ ] Verify `TimerBar` and `OptionButton` behavior with real countdown/answer states (beyond static rendering).
- [ ] Move hardcoded UI text to `res/values/strings.xml`.
- [ ] Verify dependency boundaries: UI should talk to ViewModel/Repository interfaces and prefs only where intended.

---

## 🟨 DOCS / AUDIT CLEANUP
- [ ] Update `Documents/bugs.md` to reflect current (post-skeleton) implementation state; large sections are now outdated.
- [ ] Keep TODO and blockers docs synchronized after each milestone.

---
*Updated on: March 15, 2026 (codebase re-audit)*
