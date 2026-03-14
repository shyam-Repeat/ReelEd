# Code Audit & Bug Report - ReelEd Android

**Date:** March 14, 2026
**Status:** Initial Audit (Skeleton State)

## 1. Critical Implementation Gaps (Highest Priority)
The primary "bug" currently affecting the codebase is that most source files are **empty skeletons** containing only package declarations. The app will not compile or run in its current state.

### Affected Areas:
- **`MainActivity.kt` & `App.kt`:** Missing initialization logic for Supabase, Koin/Hilt (if used), and Notification channels.
- **`OverlayForegroundService.kt`:** No implementation for managing the `WindowManager` overlay or foreground notification. This will cause the service to crash or be killed immediately by Android.
- **DAOs (`EventLogDao`, etc.):** Interfaces are defined but logic for Room database interactions is missing.
- **Repositories & API Clients:** `SupabaseClient` and `QuizRepository` lack the actual network/database logic.

## 2. Architectural Risks & Potential Bugs

### Android Service Lifecycle (Android 12+ / API 31+)
- **Bug:** `OverlayForegroundService` lacks logic to handle the strict foreground service start restrictions introduced in Android 12.
- **Risk:** Attempting to start the service from the background without a valid exception will lead to `ForegroundServiceStartNotAllowedException`.

### Trigger & Detection Logic
- **Bug:** `ForegroundAppDetector` and `VideoPlaybackDetector` are unimplemented.
- **Risk:** These components require high-privilege permissions (`UsageStats` or `AccessibilityService`). Without robust permission checking and graceful degradation logic, the app will fail to function on most devices without clear feedback to the user.

### Data Mapping & Null Safety
- **Bug:** Mapping logic between `dto` (Remote) and `entity` (Local) is missing.
- **Risk:** Android apps are prone to crashes if API responses from Supabase contain unexpected null values or if the local database schema doesn't perfectly match the remote DTOs during synchronization.

### Error Handling Strategy
- **Bug:** Zero error handling implemented for network timeouts, Supabase auth failures, or database migration errors.
- **Risk:** The app will likely experience silent failures during the `SyncWorker` process, leading to data loss or "empty" quiz screens for the child.

## 3. Project Configuration Issues
- **`node_modules/` in Root:** The presence of a `node_modules` folder in an Android Gradle project suggests leftover artifacts from a different project type or a misconfigured hybrid environment. This bloats the project and can interfere with CI/CD build times.

## 4. Recommended Fixes
1. **Implement Service Logic:** Prioritize the `OverlayForegroundService` and `OverlayLifecycleOwner` to ensure the overlay can actually be displayed.
2. **Setup Supabase Client:** Fully implement the `SupabaseClient` with the correct project URL and API keys (ensuring they are handled securely via `local.properties`).
3. **Database Layer:** Implement the Room database initialization and the mapping logic in `QuizRepository`.
4. **Cleanup:** Remove `node_modules` if this is a pure native Android project.
