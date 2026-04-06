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
- [x] Verify `TimerBar` behavior with real countdown state (wired in overlay runtime).
- [ ] Verify `OptionButton` behavior with real answer states (component exists but is not used by current quiz flow).
- [ ] Move remaining hardcoded UI text to `res/values/strings.xml`.
- [ ] Verify and tighten dependency boundaries: some UI/navigation code still accesses prefs directly.
- [x] **UI Polish (NEW):** Add a blue blur/glow to the overlay corners to improve aesthetics.
- [ ] **Horizontal Switch UI (NEW):** Further improve/standardize switch UI across screens (dashboard is styled, but some screens still use default `Switch`).

### 5) Audio & SFX (NEW)
- [x] Add the following sound files to `app/src/main/res/raw/`:
    - `sfx_correct.mp3` - Quick "Ding" for correct answers.
    - `sfx_wrong.mp3` - Short buzzer/fail sound.
    - `sfx_match.mp3` - Satisfying click/pop for tile matching.
    - `sfx_train.mp3` - Steam train "Choo-Choo" or engine sound (syncs with 1.5s entry animation).- [x] Implement `SoundManager.kt` using SoundPool.
- [x] Integrate `SoundManager` trigger calls in `TapChoiceCard`, `TapTapMatchCard`, and `TrainAnimation`.
- [x] **Background Muting (NEW):** Implement a "mute music" logic that silences background videos (e.g., Reels) while the overlay is active, making them feel paused.
- [x] **TTS for Questions (NEW):** Implement TTS for all question types. Ensure it's fast and reliable.

### 6) Parent Control & Manual Toggle (NEW)
- [x] Add a master toggle in parent controls so overlay is explicitly ON/OFF by parent.
- [x] Default overlay state to OFF for new users.
- [x] Show parent tip text to encourage enabling overlay for quizzes + notifications.
- [x] Show service notification actions only when overlay is ON.
- [x] Add configurable Daily Cap (default 15, max 20).
- [x] Add configurable Quiz Timer (default 2m, max 5m).
- [x] Add Force Quiz toggle to disable/enable 3-strike auto-dismiss behavior.

### 7) Logic Refinement (NEW)
- [x] **DrawMatchCard Logic:** Relax the pass logic in `DrawMatchCard.kt` to make it easier for users to succeed.

### 8) Supabase Cleanup (NEW)
- [x] Debug and remove unwanted/stale data from Supabase.



## 🟨 DOCS / AUDIT CLEANUP
- [x] Update `Documents/bugs.md` to reflect current (post-skeleton) implementation state.
- [x] Keep TODO and blockers docs synchronized after each milestone.

---
*Updated on: April 6, 2026 (re-audited against current code; section 4 has remaining items, section 7 remains complete)*
