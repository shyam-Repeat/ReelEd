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
- [x] Persist PIN in onboarding flow.
- [x] Wire real settings intents for:
    - [x] Overlay permission open-settings action.
    - [x] Usage access grant action.
    - [x] Notification permission action.
    - [x] Battery optimization exemption action.
- [x] Implement `OnboardingViewModel`.

### 2) Parent Dashboard integration
- [x] Connect `DashboardViewModel` to `QuizRepository`.
- [x] Implement real `showPinPrompt` flow.
- [x] Implement real `openFeedback` flow.

### 3) Overlay strict-mode behavior gaps
- [x] Enforce strict-mode-specific overlay interaction behavior (window flags now differ for strict vs non-strict).
- [x] Ensure audio muting is state-driven (active only while session + overlay are active, restored otherwise).
- [x] Ensure audio restore is guaranteed on all overlay/service teardown paths.
- [x] Implement pause/resume/extend notification action handling in service command routing.

### 4) UI quality + platform hardening
- [x] Verify `TimerBar` and `OptionButton` behavior with real countdown/answer states (beyond static rendering).
- [ ] Move hardcoded UI text to `res/values/strings.xml`.
- [ ] Verify dependency boundaries: UI should talk to ViewModel/Repository interfaces and prefs only where intended.

### 5) Audio & SFX (NEW)
- [ ] Add the following sound files to `app/src/main/res/raw/`:
    - `sfx_correct.mp3` - Quick "Ding" for correct answers.
    - `sfx_wrong.mp3` - Short buzzer/fail sound.
    - `sfx_match.mp3` - Satisfying click/pop for tile matching.
    - `sfx_train.mp3` - Steam train "Choo-Choo" or engine sound (syncs with 1.5s entry animation).
- [ ] Implement `SoundManager.kt` using SoundPool.
- [ ] Integrate `SoundManager` trigger calls in `TapChoiceCard`, `TapTapMatchCard`, and `TrainAnimation`.
- [ ] Audit `AudioMuter.kt` to ensure system-wide music is silenced correctly during quiz.



## 🟨 DOCS / AUDIT CLEANUP
- [x] Update `Documents/bugs.md` to reflect current (post-skeleton) implementation state.
- [ ] Keep TODO and blockers docs synchronized after each milestone.

---
*Updated on: March 15, 2026 (implemented onboarding/dashboard/service follow-up)*
