SIMPLE MVP TECH STACK + ARCHITECTURE FLOW
(For: AI Quiz Overlay on Social Media | 10 local Android testers | No laptop Android Studio primary workflow)

==================================================
1) CORE TECH STACK (USE THIS)
==================================================

APP (MAIN MVP)
- Kotlin
- Jetpack Compose
- Android SDK / Gradle project
- Simple MVVM architecture

ANDROID FEATURES
- SYSTEM_ALERT_WINDOW (overlay permission)
- Foreground Service
- In-app onboarding / permission guide
- Optional later only if needed:
  - UsageStatsManager (detect supported apps)
  - AccessibilityService (avoid unless absolutely required)

LOCAL STORAGE
- Room Database
- DataStore

NETWORKING
- Retrofit
- OkHttp
- Kotlin Coroutines

CLOUD / BACKEND (NO CUSTOM BACKEND FOR MVP)
- Supabase Postgres
- Use direct app -> Supabase communication

CRASH / STABILITY
- Firebase Crashlytics (recommended)

CODE GENERATION / DEV WORKFLOW
- Antigravity or Bolt (generate Kotlin code)
- GitHub (repo + version control)
- GitHub Codespaces (browser coding)
- GitHub Actions (cloud APK build)

TESTING
- Personal Android phone
- Install APK directly from GitHub Actions build artifact

==================================================
2) WHAT NOT TO USE IN MVP
==================================================

DO NOT USE
- No Node.js backend
- No Express / Fastify
- No Railway / Render / Vercel backend
- No Play Store deployment
- No real-time system
- No multiplayer
- No runtime AI generation
- No MediaProjection unless unavoidable
- No NotificationListener unless unavoidable
- No full login system unless needed

==================================================
3) SIMPLE MVP APP ARCHITECTURE
==================================================

APP LAYERS

1. UI LAYER
- Onboarding Screen
- Permission Screen
- Start / Stop Overlay Screen
- Simple Feedback Screen
- Quiz Overlay Enable Card UI (Compose)

2. SERVICE LAYER
- OverlayForegroundService
  - starts/stops overlay
  - manages timing / cooldown
  - shows quiz card over supported apps
  - logs events locally

3. DATA LAYER
- Room DB
  - cached questions
  - quiz attempts
  - unsynced events
- DataStore
  - tester_id
  - settings
  - permission flags
  - onboarding completed flag

4. NETWORK LAYER
- Retrofit API client
- Supabase REST endpoints
- Sync worker for unsynced attempts/events

==================================================
4) SIMPLE MVP FLOW (END-TO-END)
==================================================

USER INSTALLS APK
    ->
OPENS APP
    ->
ONBOARDING SCREEN
    ->
GRANT OVERLAY PERMISSION
    ->
(OPTIONAL) GRANT USAGE ACCESS LATER IF NEEDED
    ->
APP GENERATES LOCAL tester_id
    ->
APP DOWNLOADS QUIZ QUESTIONS FROM SUPABASE
    ->
CACHE QUESTIONS IN ROOM
    ->
USER TAPS "START QUIZZ(OVERLAYBUTTON) one more button with Dismiss or strict quiz"
    ->
FOREGROUND SERVICE STARTS
    ->
OVERLAY QUIZ CARD APPEARS ON TOP OF TARGET APP
    ->
USER ANSWERS / DIMISS ONLY when enabled otherwise strict show unless complete puzzle should be on screen
    ->
ATTEMPT SAVED TO ROOM
    ->
EVENT LOG SAVED TO ROOM
    ->
BACKGROUND SYNC SENDS DATA TO SUPABASE
    ->
BOLT / ANTIGRAVITY-HELPED ADMIN OR SUPABASE DASHBOARD REVIEWS DATA
    ->
YOU ANALYZE:
- answer rate
- dismiss rate
- response time
- learning tolerance
- permission friction
- retention signal

==================================================
5) MINIMUM SUPABASE TABLES
==================================================

TABLE: testers
- id
- tester_id
- nickname
- app_version
- created_at
- overlay_permission_granted
- usage_access_granted (optional)

TABLE: quiz_questions
- id
- question_text
- option_a
- option_b
- option_c
- option_d
- correct_option
- category
- difficulty
- active
- created_at

TABLE: quiz_attempts
- id
- tester_id
- question_id
- shown_at
- answered_at
- selected_option
- is_correct
- response_time_ms
- source_app
- dismissed

TABLE: overlay_sessions
- id
- tester_id
- started_at
- ended_at
- total_quizzes_shown
- total_answered
- total_dismissed

TABLE: event_logs
- id
- tester_id
- event_type
- payload_json
- created_at

TABLE: feedback
- id
- tester_id
- rating
- comment
- created_at

==================================================
6) MVP CONTENT STRATEGY (IMPORTANT)
==================================================

USE THIS
- Seed 50–100 manual quiz questions in Supabase
- Tag by:
  - category
  - difficulty
  - source_app (optional)
- Keep AI OUT of runtime for MVP

DO NOT DO YET
- No live AI generation inside app
- No personalized AI engine
- No complex adaptive learning logic

==================================================
7) REAL MVP GOAL
==================================================

YOU ARE NOT VALIDATING:
- perfect AI
- perfect automation
- perfect content understanding

YOU ARE VALIDATING:
- will users install it?
- will they grant permissions?
- will they tolerate overlay interruptions?
- will they answer enough quizzes?
- does it feel useful instead of annoying?
- is there any learning signal worth pursuing?

==================================================
8) FINAL ONE-LINE RECOMMENDATION
==================================================

BUILD:
- Kotlin + Jetpack Compose Android app
- Overlay + Foreground Service
- Room + DataStore
- Direct Supabase sync
- Code in Codespaces
- Generate with Antigravity/Bolt
- Build APK with GitHub Actions
- Test on your Android phone

THIS IS THE CLEANEST SIMPLE MVP STACK FOR YOUR IDEA