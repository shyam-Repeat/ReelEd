# Code Audit & Bug Report - ReelEd Android

**Date:** March 15, 2026
**Status:** Post-implementation Audit

## 1. Current State Summary
The project is no longer in skeleton-only state. Core MVP implementation now exists across:
- App entry, onboarding routing, and child/parent flows.
- Room local data and Supabase remote sync path.
- Workers for question fetch, sync, and watchdog restart.
- Overlay foreground service with trigger checks and quiz rendering.

## 2. Remaining Risks / Follow-ups

### UI hardening
- `TimerBar` and `OptionButton` need broader runtime verification with live countdown and answer state transitions across all quiz card types.

### String resources
- Multiple onboarding/dashboard/service labels are still inline literals and should be migrated to `res/values/strings.xml`.

### Architecture boundaries
- A few screens still instantiate concrete dependencies directly; consider introducing explicit dependency injection for cleaner testability.

## 3. Service/Trigger Notes
- Overlay notification actions now route pause/resume/extend commands through the service.
- Audio muting is state-driven by session + overlay visibility to reduce stuck-mute behavior.
- Strict mode and non-strict mode now use differentiated overlay window flags.

## 5. Resolved Issues (March 25, 2026)

### Overlay Dismissal Failure
- **Issue:** Quiz overlay stayed on screen even after correct answers or timer expiration.
- **Cause:** `reconcileOverlayState` in `OverlayForegroundService` was nullifying the `overlayView` reference if the view wasn't "fully attached" during the first few seconds of a quiz session. This orphaned the view from the service's control.
- **Fix:** Removed premature nullification of `overlayView` and added window token checks in `removeOverlayIfShowing`.

### App Switch Sensitivity
- **Issue:** Quiz was sometimes dismissed immediately after opening.
- **Cause:** `ForegroundAppDetector` reported transient `null` package names during app transitions, which the service interpreted as the user leaving the target app.
- **Fix:** Refined app switch logic to only dismiss if a known *different* target app is clearly in the foreground, ignoring transient nulls/system transitions.

### Strike Counting in Matching Quizzes
- **Issue:** Incorrect matches in `TapTapMatchCard` didn't count toward the 3-strike limit.
- **Cause:** Missing callback to `onResult` for incorrect match attempts in the matching card implementation.
- **Fix:** Added `onResult` reporting for incorrect matches to ensure consistent strike behavior across all quiz types.

### Timer/Completion Race Condition
- **Issue:** Potential double-triggering of "Close" events if a correct answer and timeout happened near-simultaneously.
- **Fix:** Updated `QuizCardRouter` to set `quizFinished = true` immediately upon any result, and added a view-lock check in `onQuizResult` to ensure only the first completion event is processed.

