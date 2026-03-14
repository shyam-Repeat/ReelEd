BLOCKERS + SIMPLE AVOID FLOW

1. User Permission Friction
Blocker:
Users may deny overlay / usage access / notification permissions.
Avoid:
Clear parent-focused explanation + choose only Instagram/YouTube + adult-only disable.

2. Play Store Accessibility Risk
Blocker:
Using Accessibility to monitor reels or simulate taps can cause rejection.
Avoid:
Do not use Accessibility in MVP.

3. Detecting Instagram / YouTube Open
Blocker:
Need to know if app is active.
Avoid:
Use PACKAGE_USAGE_STATS (foreground app detection).

4. Detecting Reels / Video Playing
Blocker:
No official API for exact Reels / Shorts detection.
Avoid:
Use MediaSession / notification listener + foreground app + timer heuristic.

5. Auto Pause / Resume Not Allowed
Blocker:
Cannot directly pause Instagram reels or YouTube shorts.
Avoid:
Use full-screen opaque overlay + mute audio to create “fake pause”.

6. OEM Battery Kill (Xiaomi/Oppo/Vivo etc.)
Blocker:
Phone may kill monitoring service.
Avoid:
Foreground service + persistent notification + onboarding to disable battery optimization + WorkManager watchdog.

7. Overlay Touch / Gesture Issues
Blocker:
Drag/swipe can fail or pass through to underlying app.
Avoid:
Use full-screen opaque overlay + tap-only quiz + correct overlay flags + consume all touches.

8. Play Store “Interfering with Other Apps” Risk
Blocker:
Reviewer may think app disrupts Instagram/YouTube.
Avoid:
Position as parental education tool + clear opt-in + overlay only on selected apps + transparent purpose.

9. MediaProjection Fake Freeze Risk
Blocker:
Screen capture requires recording permission and may trigger Play review concerns.
Avoid:
Do not use MediaProjection for MVP.

10. Child Compliance (Families / COPPA / GDPR-K)
Blocker:
Kids app gets stricter review.
Avoid:
Parent-controlled setup + minimal data collection + clear consent flow.