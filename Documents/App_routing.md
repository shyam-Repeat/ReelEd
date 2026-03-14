```kotlin
fun getStartDestination(prefs: AppPrefs): String {
    return when {
        !prefs.onboardingComplete      -> "onboarding"        // Blockers 1, 6, 10
        !prefs.pinSet                  -> "pin_setup"         // Blocker 4
        !prefs.consentGiven            -> "consent"           // Blocker 10
        !prefs.hasOverlayPermission    -> "permission_overlay"// Blocker 1
        !prefs.hasUsageStatsPermission -> "permission_usage"  // Blockers 1, 3
        else                           -> "child_home"        // Normal operation
    }
}
```

Complete onboarding screen order:
```
1. Welcome screen
2. Parental consent (Blocker 10)
3. PIN setup (Blocker 4 / Section 4)
4. Overlay permission (Blocker 1)
5. Usage access permission (Blockers 1, 3)
6. Notification permission (Blocker 1)
7. Battery optimization (Blocker 6)
8. Done → child_home
```

---

### 6.2 BLOCKER COVERAGE SUMMARY

| # | Blocker | Covered By |
|---|---|---|
| 1 | Permission friction | Section 6, Blocker 1 — step-by-step onboarding flow |
| 2 | Accessibility risk | Excluded from entire document |
| 3 | Detect Instagram/YouTube open | Section 6, Blocker 3 — ForegroundAppDetector |
| 4 | Detect Reels/video playing | Section 6, Blocker 4 — VideoPlaybackDetector 3-signal |
| 5 | Fake pause | Section 6, Blocker 5 — strict mode flags + audio mute |
| 6 | OEM battery kill | Section 6, Blocker 6 — 3-layer defense |
| 7 | Touch/gesture issues | Section 6, Blocker 7 — mode-aware touch flags |
| 8 | Play Store risk | Section 6, Blocker 8 — manifest + listing strategy |
| 9 | MediaProjection | Excluded from entire document |
| 10 | COPPA/GDPR-K | Section 6, Blocker 10 — data minimization + consent |

---