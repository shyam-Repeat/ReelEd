# MVP FUNCTIONS DEFINITION
## AI Quiz Overlay — Android MVP | 10 Testers Build
### Version: R1 | Configuration-Driven Architecture

---

## TABLE OF CONTENTS

1. Quiz Card Overlay UI — Configuration-Driven Architecture
2. Parent Dashboard — In-App Simple View
3. Trigger Logic Algorithm — No AI
4. Parent Disable Control Mechanism
5. Foreground & Background Data Services

---

---

# SECTION 1: QUIZ CARD OVERLAY UI
## Configuration-Driven / Data-Driven Architecture

---

### 1.1 CORE DESIGN PRINCIPLE

The quiz card is 100% driven by a QuizCardConfig JSON object.
The UI does NOT hard-code any quiz type. Instead:

- Supabase sends a question with a `card_type` field
- The app reads `card_type` and renders the matching Composable
- New quiz types can be added without changing Android code — only the JSON changes

This is the configuration-driven / data-driven architecture.

---

### 1.2 QUIZ CARD CONFIG — MASTER JSON SCHEMA

Every question row in Supabase and Room must carry this shape:

```json
{
  "id": "q_001",
  "card_type": "TAP_CHOICE",
  "subject": "math",
  "difficulty": 1,
  "active": true,
  "display": {
    "question_text": "What is 6 × 7?",
    "instruction_label": "Tap the correct answer",
    "media_url": null
  },
  "payload": {
    "options": [
      { "id": "A", "label": "42", "is_correct": true },
      { "id": "B", "label": "36", "is_correct": false },
      { "id": "C", "label": "48", "is_correct": false },
      { "id": "D", "label": "54", "is_correct": false }
    ]
  },
  "rules": {
    "timer_seconds": 20,
    "strict_mode": false,
    "max_attempts": 1,
    "show_correct_on_wrong": true
  }
}
```

Field definitions:

| Field | Type | Purpose |
|---|---|---|
| card_type | String ENUM | Which Composable to render |
| display.question_text | String | Main question shown to child |
| display.instruction_label | String | Sub-label ("Tap", "Drag", etc.) |
| display.media_url | String? | Optional image/icon URL |
| payload | Object | Type-specific data (options, pairs, blanks) |
| rules.timer_seconds | Int | 0 = no timer |
| rules.strict_mode | Boolean | If true, no dismiss button shown |
| rules.show_correct_on_wrong | Boolean | Reveal answer after wrong tap |

---

### 1.3 CARD TYPE ENUM — 4 FIXED TYPES

```kotlin
enum class QuizCardType {
    TAP_CHOICE,      // 4-option MCQ tap
    TAP_TAP_MATCH,   // Two columns, tap left then tap right to pair
    DRAG_DROP_MATCH, // Drag item from left, drop onto right slot
    FILL_BLANK       // Short text or word bank fill-in
}
```

Each type maps to exactly one Composable function (see 1.5).

---

### 1.4 CARD SIZE & OVERLAY CONSTRAINTS

Why these exact dimensions:
- Instagram/YouTube cannot detect or block a floating window
  drawn via SYSTEM_ALERT_WINDOW as long as it does NOT
  cover the entire screen (full-screen blocks are flagged
  by some OEMs as screen captures)
- Bottom-anchored card leaves the top content visible,
  making it feel less hostile to the child

```
OVERLAY CARD DIMENSIONS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Position    : Bottom of screen, anchored
Width       : 100% screen width
Height      : 52% to 58% of screen height MAX
             (leaves ~42% of host app visible at top)
Corner Rx   : 20dp top corners only
Background  : Solid white or solid dark — no blur
             (blur requires API 31+ and causes jank)
Elevation   : WindowManager.LayoutParams TYPE_APPLICATION_OVERLAY
Z-order     : Always on top via WindowManager flag
Padding     : 20dp all sides inside card
Animation   : Slide up from bottom — 280ms ease-out
Dismiss anim: Slide down — 200ms ease-in
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

WindowManager params (Kotlin):
```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    (screenHeight * 0.56).toInt(),
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.BOTTOM
    y = 0
}
```

FLAG_NOT_FOCUSABLE is critical: it lets touch events pass
through the transparent top portion to the host app (Instagram).
The card itself still receives touches inside its own bounds.

---

### 1.5 COMPOSABLE FUNCTION DEFINITIONS — 4 CARD TYPES

---

#### TYPE 1: TAP_CHOICE (Standard MCQ)

Use case: Math facts, general knowledge, vocabulary
Best for: Quick interruption, under 20 seconds

```
VISUAL LAYOUT (top to bottom inside card)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ Timer bar — thin progress strip, fills left to right ]
[ Question Text — 18sp bold, max 2 lines               ]
[ Instruction label — 12sp muted "Tap the answer"      ]
──────────────────────────────────────────────────────
[ Option A button ]  [ Option B button ]
[ Option C button ]  [ Option D button ]
──────────────────────────────────────────────────────
[ Dismiss link — tiny, right-aligned, only if !strict ]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

```kotlin
@Composable
fun TapChoiceCard(
    config: QuizCardConfig,
    onAnswered: (selectedId: String, isCorrect: Boolean, responseTimeMs: Long) -> Unit,
    onDismissed: () -> Unit,
    onTimerExpired: () -> Unit
) {
    // State
    var selectedId by remember { mutableStateOf<String?>(null) }
    var revealed by remember { mutableStateOf(false) }
    val startTime = remember { System.currentTimeMillis() }

    // Timer countdown (uses config.rules.timer_seconds, 0 = disabled)
    // On expire: call onTimerExpired(), lock all buttons

    // Option button colors
    // Default    : outline border, white fill
    // Selected   : primary blue fill
    // Correct    : green fill (after reveal)
    // Wrong      : red fill (after reveal)

    // On tap:
    // 1. Set selectedId
    // 2. Calculate responseTimeMs = now - startTime
    // 3. If show_correct_on_wrong && wrong: set revealed = true, wait 1500ms, then onAnswered()
    // 4. If correct: flash green, wait 800ms, then onAnswered()
    // 5. onAnswered passes selectedId, isCorrect, responseTimeMs up to service
}
```

Payload used from JSON:
```json
"payload": {
  "options": [
    { "id": "A", "label": "42", "is_correct": true },
    { "id": "B", "label": "36", "is_correct": false },
    { "id": "C", "label": "48", "is_correct": false },
    { "id": "D", "label": "54", "is_correct": false }
  ]
}
```

---

#### TYPE 2: TAP_TAP_MATCH (Two-column pairing)

Use case: Word-definition pairs, math equation matching,
         language translation matching
Best for: Vocabulary, language learning, age 8+

```
VISUAL LAYOUT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ Question Text                         ]
[ "Match each word to its meaning"      ]
──────────────────────────────────────────────────────
LEFT COLUMN          RIGHT COLUMN
[ Dog     ] (tap1)   [ Animal  ] (tap2 → draws line)
[ Run     ]          [ Move fast ]
[ Big     ]          [ Large    ]
[ Eat     ]          [ Food     ]
──────────────────────────────────────────────────────
Matched pairs get a connecting line drawn between them
Wrong pair: line flashes red and resets
All correct: green lines, auto-advance after 600ms
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

```kotlin
@Composable
fun TapTapMatchCard(
    config: QuizCardConfig,
    onAnswered: (allCorrect: Boolean, responseTimeMs: Long) -> Unit,
    onDismissed: () -> Unit
) {
    // State
    var selectedLeft by remember { mutableStateOf<String?>(null) }
    var matchedPairs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // matchedPairs = { leftId -> rightId } for confirmed correct pairs

    // Tap logic:
    // 1. Tap a left item → highlight it, set selectedLeft
    // 2. Tap a right item with selectedLeft set:
    //    a. Check if left.correct_match == right.id
    //    b. Correct: add to matchedPairs, draw green line, clear selectedLeft
    //    c. Wrong: flash red on both, clear selectedLeft after 600ms
    // 3. When matchedPairs.size == total pairs: all correct → onAnswered(true, elapsed)

    // Line drawing: Canvas composable overlaid on the two columns
    // Draw lines between matched pair center-points
    // Use animateColorAsState for green/red flash
}
```

Payload used from JSON:
```json
"payload": {
  "pairs": [
    { "left_id": "L1", "left_label": "Dog",  "right_id": "R3", "right_label": "Animal" },
    { "left_id": "L2", "left_label": "Run",  "right_id": "R1", "right_label": "Move fast" },
    { "left_id": "L3", "left_label": "Big",  "right_id": "R4", "right_label": "Large" },
    { "left_id": "L4", "left_label": "Eat",  "right_id": "R2", "right_label": "Food" }
  ],
  "right_order_shuffled": ["R2", "R4", "R1", "R3"]
}
```

Note: right_order_shuffled is pre-shuffled in Supabase so the
app doesn't need to shuffle at runtime (simpler, reproducible).

---

#### TYPE 3: DRAG_DROP_MATCH (Drag to slot)

Use case: Ordering steps, filling categories,
         sorting numbers/words
Best for: Age 9+, longer engagement windows (30+ sec on screen)

```
VISUAL LAYOUT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ Question: "Put these numbers in order" ]
──────────────────────────────────────────────────────
DRAG CHIPS (source pool — top area, wrapping row):
[ 7 ]  [ 2 ]  [ 9 ]  [ 4 ]

DROP SLOTS (destination — below, labeled):
Slot 1: [ _____ ]   Slot 2: [ _____ ]
Slot 3: [ _____ ]   Slot 4: [ _____ ]

Dropped chip sits inside slot.
Tap a filled slot to eject chip back to pool.
Submit button appears when all slots filled.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

```kotlin
@Composable
fun DragDropMatchCard(
    config: QuizCardConfig,
    onAnswered: (isCorrect: Boolean, responseTimeMs: Long) -> Unit,
    onDismissed: () -> Unit
) {
    // State
    var slotContents by remember { mutableStateOf<Map<String, String?>>(
        config.payload.slots.associate { it.slot_id to null }
    )}
    var dragItem by remember { mutableStateOf<String?>(null) }

    // Drag gesture: use pointerInput + detectDragGesturesAfterLongPress
    // Track drag offset. On release, hit-test against slot bounding boxes.
    // If hit: fill slot (if empty), remove chip from pool
    // If miss: return chip to pool with spring animation

    // Submit logic:
    // All slots filled → show Submit button
    // On Submit: compare slotContents to payload.slots[].correct_chip_id
    // All match = correct, else wrong
    // Show correct slots green, wrong slots red for 1500ms then onAnswered()

    // IMPORTANT: Keep drag detection threshold at 8dp minimum
    // to avoid false triggers while child scrolls the quiz card itself
}
```

Payload used from JSON:
```json
"payload": {
  "chips": [
    { "chip_id": "C1", "label": "2" },
    { "chip_id": "C2", "label": "4" },
    { "chip_id": "C3", "label": "7" },
    { "chip_id": "C4", "label": "9" }
  ],
  "slots": [
    { "slot_id": "S1", "slot_label": "Smallest", "correct_chip_id": "C1" },
    { "slot_id": "S2", "slot_label": "2nd",       "correct_chip_id": "C2" },
    { "slot_id": "S3", "slot_label": "3rd",       "correct_chip_id": "C3" },
    { "slot_id": "S4", "slot_label": "Largest",   "correct_chip_id": "C4" }
  ]
}
```

---

#### TYPE 4: FILL_BLANK (Word bank tap)

Use case: Sentence completion, vocabulary, spelling
Best for: Language, English, early reading

```
VISUAL LAYOUT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ "The ___ is the closest star to Earth." ]
                        ↑ blank = tappable underline slot

WORD BANK (chips below sentence):
[ Moon ]  [ Sun ]  [ Mars ]  [ Earth ]

Tap a word chip → fills the blank
Tap the filled blank → ejects word back to bank
Submit button appears when blank is filled.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

For MVP: maximum 1 blank per sentence (simpler state).
Multi-blank is post-MVP.

```kotlin
@Composable
fun FillBlankCard(
    config: QuizCardConfig,
    onAnswered: (isCorrect: Boolean, responseTimeMs: Long) -> Unit,
    onDismissed: () -> Unit
) {
    // State
    var blankFilled by remember { mutableStateOf<String?>(null) }

    // Render sentence by splitting on "___"
    // Parts before and after blank render as Text
    // Blank slot renders as underlined clickable Box
    // Word bank chips render below as tappable Chips

    // On chip tap: set blankFilled = chip_id, hide chip from bank
    // On blank tap when filled: return chip to bank, clear blankFilled
    // Submit: compare blankFilled to payload.correct_chip_id
}
```

Payload used from JSON:
```json
"payload": {
  "sentence_template": "The ___ is the closest star to Earth.",
  "blank_position": 0,
  "word_bank": [
    { "chip_id": "W1", "label": "Moon" },
    { "chip_id": "W2", "label": "Sun",  "is_correct": true },
    { "chip_id": "W3", "label": "Mars" },
    { "chip_id": "W4", "label": "Earth" }
  ]
}
```

---

### 1.6 CARD TYPE ROUTER FUNCTION

Single function that reads card_type and dispatches to the
correct Composable. This is the configuration-driven layer:

```kotlin
@Composable
fun QuizCardRouter(
    config: QuizCardConfig,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    when (config.cardType) {
        QuizCardType.TAP_CHOICE      -> TapChoiceCard(config, onResult, onDismissed)
        QuizCardType.TAP_TAP_MATCH   -> TapTapMatchCard(config, onResult, onDismissed)
        QuizCardType.DRAG_DROP_MATCH -> DragDropMatchCard(config, onResult, onDismissed)
        QuizCardType.FILL_BLANK      -> FillBlankCard(config, onResult, onDismissed)
    }
}
```

```kotlin
data class QuizAttemptResult(
    val questionId: String,
    val selectedOptionId: String?,   // null if timer expired
    val isCorrect: Boolean,
    val wasDismissed: Boolean,
    val responseTimeMs: Long,
    val sourceApp: String            // e.g. "com.instagram.android"
)
```

---

### 1.7 CARD STATE MACHINE

Every quiz card goes through exactly these states:

```
IDLE
  │
  ▼ triggerQuiz() called by OverlayForegroundService
APPEARING  ← slide-up animation (280ms)
  │
  ▼ animation complete
ACTIVE     ← child interacting, timer running
  │
  ├── child answers → RESULT_FLASH (800ms green/red) → DISMISSED_CORRECT
  ├── timer expires → RESULT_FLASH (show answer) → DISMISSED_TIMEOUT
  └── child dismisses (if !strict) → DISMISSED_MANUAL
  │
  ▼ any DISMISSED state
DISAPPEARING ← slide-down animation (200ms)
  │
  ▼
IDLE
```

The OverlayForegroundService listens for any DISMISSED state
and writes QuizAttemptResult to Room immediately.

---

---

# SECTION 2: PARENT DASHBOARD
## Simple In-App View

---

### 2.1 DESIGN RULE

Parent dashboard = 1 screen, no navigation.
Everything a parent needs to validate "is my kid learning?" 
fits in a single scrollable screen.
No graphs, no charts, no pagination for MVP.

---

### 2.2 SCREEN LAYOUT

```
PARENT DASHBOARD SCREEN — Single scroll view
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─────────────────────────────────────────┐
│  Today's Summary                        │
│  ─────────────────────────────────────  │
│  Quizzes shown      :  8                │
│  Answered           :  6    (75%)       │
│  Correct            :  4    (67%)       │
│  Dismissed          :  2                │
│  Avg response time  :  9.4 sec          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  This Week                              │
│  ─────────────────────────────────────  │
│  Mon  ████████░░  8/10 correct          │
│  Tue  ██████░░░░  6/10 correct          │
│  Wed  ░░░░░░░░░░  No session            │
│  Thu  █████████░  9/10 correct  ← today │
│  Fri  ──                                │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  By Subject — Today                     │
│  ─────────────────────────────────────  │
│  Math        :  3 correct / 4 shown     │
│  English     :  1 correct / 2 shown     │
│  General     :  0 correct / 2 shown     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Recent Attempts (last 5)               │
│  ─────────────────────────────────────  │
│  ✓  What is 6×7?          3.2s  Math    │
│  ✗  Capital of France?    12s   General │
│  ✓  Fill: The ___ star    7.8s  English │
│  —  (dismissed)                         │
│  ✓  Match animals         15s   Science │
└─────────────────────────────────────────┘

[ DISABLE OVERLAY  ▶ ]   [ SEND FEEDBACK ]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 2.3 COMPOSABLE FUNCTION DEFINITIONS

```kotlin
@Composable
fun ParentDashboardScreen(
    viewModel: ParentDashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn {
        item { TodaySummaryCard(uiState.todaySummary) }
        item { WeekBarCard(uiState.weekData) }
        item { SubjectBreakdownCard(uiState.todayBySubject) }
        item { RecentAttemptsCard(uiState.recentAttempts) }
        item { ActionButtons(onDisable = viewModel::showPinPrompt,
                             onFeedback = viewModel::openFeedback) }
    }
}
```

```kotlin
@Composable
fun TodaySummaryCard(summary: TodaySummary) {
    // Shows: shown, answered, correct, dismissed, avg response time
    // All from Room query: SELECT * FROM quiz_attempts WHERE date(shown_at) = today
    // No network call — all local Room data
    Card { ... }
}
```

```kotlin
@Composable
fun WeekBarCard(weekData: List<DaySummary>) {
    // Simple horizontal bar per day using Box + fillMaxWidth(fraction)
    // fraction = correctCount / shownCount
    // No chart library needed — pure Compose Box layout
    // 7 rows max (Mon–Sun)
    Card { ... }
}
```

```kotlin
@Composable
fun SubjectBreakdownCard(subjects: List<SubjectStat>) {
    // Simple text rows: "Math: 3 correct / 4 shown"
    // Derived from quiz_attempts JOIN quiz_questions ON category
    // Local Room query only
    Card { ... }
}
```

```kotlin
@Composable
fun RecentAttemptsCard(attempts: List<AttemptPreview>) {
    // Last 5 attempts from Room, newest first
    // Each row: icon (✓/✗/—) + question_text truncated + response_time + category
    Card { ... }
}
```

---

### 2.4 VIEWMODEL + DATA QUERIES

```kotlin
class ParentDashboardViewModel(private val repo: QuizRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getTodaySummary(),
        repo.getWeekSummary(),
        repo.getTodayBySubject(),
        repo.getRecentAttempts(limit = 5)
    ) { today, week, subjects, recent ->
        DashboardUiState(today, week, subjects, recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Empty)

}
```

All queries are Room Flows — reactive, no polling, no network.

```kotlin
// Room DAO queries needed:

@Query("""
    SELECT COUNT(*) as shown,
           SUM(CASE WHEN dismissed = 0 THEN 1 ELSE 0 END) as answered,
           SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct,
           SUM(CASE WHEN dismissed = 1 THEN 1 ELSE 0 END) as dismissed,
           AVG(response_time_ms) as avg_response_ms
    FROM quiz_attempts
    WHERE date(shown_at / 1000, 'unixepoch') = date('now')
""")
fun getTodaySummary(): Flow<TodaySummary>

@Query("""
    SELECT date(shown_at / 1000, 'unixepoch') as day,
           COUNT(*) as shown,
           SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as correct
    FROM quiz_attempts
    WHERE shown_at >= :weekStartMs
    GROUP BY day ORDER BY day ASC
""")
fun getWeekSummary(weekStartMs: Long): Flow<List<DaySummary>>

@Query("""
    SELECT q.category, COUNT(*) as shown,
           SUM(CASE WHEN a.is_correct = 1 THEN 1 ELSE 0 END) as correct
    FROM quiz_attempts a
    JOIN quiz_questions q ON a.question_id = q.id
    WHERE date(a.shown_at / 1000, 'unixepoch') = date('now')
    GROUP BY q.category
""")
fun getTodayBySubject(): Flow<List<SubjectStat>>

@Query("""
    SELECT a.*, q.question_text, q.category
    FROM quiz_attempts a
    JOIN quiz_questions q ON a.question_id = q.id
    ORDER BY a.shown_at DESC LIMIT :limit
""")
fun getRecentAttempts(limit: Int): Flow<List<AttemptPreview>>
```

---

---

# SECTION 3: TRIGGER LOGIC ALGORITHM
## No AI, Rule-Based, Designed for Learning Signal

---

### 3.1 DESIGN PRINCIPLES

- Trigger often enough to gather data across 10 testers
- Not so often that parents disable the app in day 1
- Respect the child's session — mid-scroll interruption
  is the point, but catastrophic interruption kills trust
- All state stored in DataStore (lightweight, no Room needed)

---

### 3.2 TRIGGER ALGORITHM — COMPLETE RULES

```
TRIGGER ENGINE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

INPUT SIGNALS (all tracked by Foreground Service):
  - session_start_time     : when child opened target app
  - last_quiz_shown_time   : epoch ms of last quiz
  - quizzes_shown_today    : count, resets at midnight
  - last_answer_was_correct: boolean
  - overlay_active         : boolean (quiz on screen now)

HARD GATES (check first — if ANY is true, do NOT trigger):
  1. overlay_active == true            (already showing quiz)
  2. quizzes_shown_today >= MAX_DAILY  (daily cap reached)
  3. now - last_quiz_shown_time < COOLDOWN_MS (cooldown active)
  4. now - session_start_time < WARMUP_MS     (too soon in session)

IF all gates pass → check TRIGGER CONDITION:
  5. now - session_start_time >= TRIGGER_AFTER_MS
     AND
     now - last_quiz_shown_time >= INTERVAL_MS

IF condition met → FIRE QUIZ

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 3.3 TIMING CONSTANTS (MVP VALUES)

These are tuned for 10-tester learning signal collection.
Adjust after week 1 based on dismiss rate data.

```kotlin
object TriggerConfig {

    // How long child must be in app before FIRST quiz fires
    val WARMUP_MS = 3 * 60 * 1000L          // 3 minutes

    // Minimum time between any two quizzes
    val COOLDOWN_MS = 12 * 60 * 1000L       // 12 minutes

    // How long into a session before quiz is eligible again
    val INTERVAL_MS = 10 * 60 * 1000L       // 10 minutes

    // Max quizzes per day per child (resets at midnight)
    val MAX_DAILY = 6

    // How often the Foreground Service checks trigger conditions
    val POLLING_INTERVAL_MS = 30 * 1000L    // every 30 seconds

    // If child dismissed last quiz, extend cooldown by this
    val DISMISS_PENALTY_MS = 8 * 60 * 1000L // 8 extra minutes

}
```

Rationale:
- 3-min warmup: child settles into app, less hostile to interruption
- 12-min cooldown: parent-friendly, feels like ad breaks on TV
- Max 6/day: at 12-min intervals, max possible = 6 in ~72 min session
- Dismiss penalty: if child dismissed, wait longer before retrying

---

### 3.4 QUESTION SELECTION ALGORITHM

```kotlin
fun selectNextQuestion(
    allCachedQuestions: List<QuizQuestion>,
    attemptedTodayIds: Set<String>,
    lastShownId: String?
): QuizQuestion {

    // Rule 1: Never repeat a question shown today
    val notSeenToday = allCachedQuestions.filter {
        it.id !in attemptedTodayIds
    }

    // Rule 2: Never show the same question back-to-back
    val candidates = notSeenToday.filter { it.id != lastShownId }

    // Rule 3: If no unseen questions remain, reset pool
    //         (only happens if MAX_DAILY > question pool size, unlikely)
    val pool = candidates.ifEmpty { allCachedQuestions.filter { it.id != lastShownId } }

    // Rule 4: Difficulty progression within a day
    //   - First 2 quizzes of day → difficulty == 1 (easy)
    //   - Quiz 3-4              → difficulty == 1 or 2
    //   - Quiz 5+               → any difficulty
    val todayCount = attemptedTodayIds.size
    val difficultyPool = when {
        todayCount < 2  -> pool.filter { it.difficulty == 1 }
        todayCount < 4  -> pool.filter { it.difficulty <= 2 }
        else            -> pool
    }.ifEmpty { pool }

    // Rule 5: Balance subjects across the day
    //   Pick the subject that has been shown LEAST today
    //   Simple round-robin across [math, english, general, science]
    val leastShownSubject = getLeastShownSubjectToday(attemptedTodayIds, allCachedQuestions)
    val subjectFiltered = difficultyPool.filter { it.subject == leastShownSubject }
        .ifEmpty { difficultyPool }

    // Final: random pick from filtered pool
    return subjectFiltered.random()
}
```

```kotlin
fun getLeastShownSubjectToday(
    shownIds: Set<String>,
    allQuestions: List<QuizQuestion>
): String {
    val subjects = listOf("math", "english", "science", "general")
    val shownQuestions = allQuestions.filter { it.id in shownIds }
    val countBySubject = subjects.associateWith { subject ->
        shownQuestions.count { it.subject == subject }
    }
    return countBySubject.minByOrNull { it.value }?.key ?: subjects.random()
}
```

---

### 3.5 TRIGGER CHECKER — SERVICE LOOP

```kotlin
class TriggerEngine(
    private val prefs: TriggerPrefs,    // DataStore wrapper
    private val roomDao: QuizDao
) {

    // Called by OverlayForegroundService every POLLING_INTERVAL_MS
    suspend fun checkAndFire(): TriggerDecision {

        val now = System.currentTimeMillis()
        val state = prefs.getTriggerState() // reads DataStore

        // Gate 1: Already showing
        if (state.overlayActive) return TriggerDecision.Skip("overlay_active")

        // Gate 2: Daily cap
        if (state.quizzesShownToday >= TriggerConfig.MAX_DAILY)
            return TriggerDecision.Skip("daily_cap_reached")

        // Gate 3: Cooldown (including dismiss penalty if applicable)
        val effectiveCooldown = if (state.lastWasDismissed)
            TriggerConfig.COOLDOWN_MS + TriggerConfig.DISMISS_PENALTY_MS
        else TriggerConfig.COOLDOWN_MS
        if (now - state.lastQuizShownTime < effectiveCooldown)
            return TriggerDecision.Skip("cooldown_active")

        // Gate 4: Session warmup
        if (now - state.sessionStartTime < TriggerConfig.WARMUP_MS)
            return TriggerDecision.Skip("warmup_not_done")

        // Gate 5: Interval since last quiz
        if (now - state.lastQuizShownTime < TriggerConfig.INTERVAL_MS)
            return TriggerDecision.Skip("interval_not_elapsed")

        // All gates passed — select question
        val question = selectNextQuestion(
            allCachedQuestions = roomDao.getAllActive(),
            attemptedTodayIds = roomDao.getAttemptedIdsToday(),
            lastShownId = state.lastShownQuestionId
        )

        return TriggerDecision.Fire(question)
    }
}

sealed class TriggerDecision {
    data class Fire(val question: QuizQuestion) : TriggerDecision()
    data class Skip(val reason: String) : TriggerDecision()
}
```

---

---

# SECTION 4: PARENT DISABLE CONTROL MECHANISM
## Simple PIN-Based Lock, MVP

---

### 4.1 DESIGN RULE

- Same app used by parent and child
- Child must NOT be able to disable the overlay
- Solution: 4-digit PIN gate on all control actions
- PIN is set during parent onboarding
- PIN stored in DataStore (hashed with SHA-256, never plain text)
- No biometrics for MVP (adds complexity, BiometricPrompt API varies by device)

---

### 4.2 CONTROL ACTIONS THAT REQUIRE PIN

```kotlin
enum class ProtectedAction {
    DISABLE_OVERLAY,         // Stop Foreground Service
    CHANGE_SETTINGS,         // Adjust trigger timing
    VIEW_DASHBOARD,          // Parent-only data view
    UNINSTALL_GUARD          // See Section 4.5
}
```

---

### 4.3 PIN FLOW COMPOSABLE

```kotlin
@Composable
fun PinGateDialog(
    action: ProtectedAction,
    onPinCorrect: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var attempts by remember { mutableStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var lockoutEndsAt by remember { mutableStateOf(0L) }

    // Layout:
    // Title: "Parent PIN required"
    // Subtitle: action description
    // 4 dot indicators (filled/empty based on enteredPin.length)
    // 10-key numpad (0-9) + backspace
    // Cancel button

    // On 4th digit entered:
    //   hash enteredPin with SHA-256
    //   compare to stored hash in DataStore
    //   if match: dismiss dialog, call onPinCorrect()
    //   if no match:
    //     attempts++
    //     shake animation on dots
    //     clear enteredPin
    //     if attempts >= 3: locked = true, lockoutEndsAt = now + 60_000

    // Lockout state: show countdown "Try again in 45s"
    // Use LaunchedEffect + ticker to count down
}
```

---

### 4.4 PIN SETUP (ONBOARDING STEP)

```kotlin
@Composable
fun PinSetupScreen(
    onPinSet: () -> Unit
) {
    // Step 1: "Create a 4-digit parent PIN"
    //         4-digit numpad entry → stored as firstPin
    // Step 2: "Confirm PIN"
    //         numpad entry again → if matches firstPin: hash and save
    //         if no match: shake + "PINs don't match, try again"
    //         reset to Step 1

    // On success: save SHA-256(pin) to DataStore → onPinSet()
}
```

```kotlin
// DataStore keys
object PinKeys {
    val PARENT_PIN_HASH = stringPreferencesKey("parent_pin_hash")
    val PIN_SET = booleanPreferencesKey("pin_set")
    val FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
    val LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
}
```

---

### 4.5 DISABLE OVERLAY FLOW (COMPLETE)

When parent taps "Disable Overlay" button on dashboard:

```
1. PinGateDialog appears
2. Parent enters PIN
3. On correct PIN:
   a. OverlayForegroundService.stopSelf() called
   b. DataStore: overlay_enabled = false
   c. UI shows "Overlay disabled" with [Re-enable] button
   d. [Re-enable] also requires PIN

When child opens app:
   - App shows a simple "Learning mode paused" screen
   - No dashboard visible
   - No settings visible
   - Only a home screen with app icon and status message
   - No obvious way to reach parent controls without PIN
```

---

### 4.6 APP ENTRY ROUTING LOGIC

```kotlin
// MainActivity.kt — on launch, route based on state

fun getStartDestination(prefs: AppPrefs): String {
    return when {
        !prefs.onboardingComplete    -> "onboarding"
        !prefs.pinSet                -> "pin_setup"
        prefs.overlayEnabled         -> "child_home"   // simple status screen
        else                         -> "child_home"   // overlay disabled state
    }
}

// Parent dashboard is NEVER the start destination
// Parent must navigate via a hidden/discreet entry point:
// Example: tap app icon 3 times rapidly → PIN prompt → dashboard
// OR: long press on a specific UI element → PIN prompt → dashboard
// For MVP: a small "Parent" text link at bottom of child_home screen → PIN → dashboard
```

---

### 4.7 WHAT CHILD SEES vs PARENT SEES

```
CHILD HOME SCREEN (default after onboarding)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ App logo / friendly icon                ]
[ "Learning mode is ON!"                  ]
[ "Quizzes will pop up while you scroll." ]
[                                         ]
[                                         ]
[      (no buttons, no controls)          ]
[                                         ]
[ small muted link: "Parent? Tap here"   ]  ← leads to PIN gate
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

AFTER PIN → PARENT DASHBOARD (Section 2)
```

---

---

# SECTION 5: FOREGROUND & BACKGROUND DATA SERVICES
## Minimal Usage, Local-First, Smart Sync

---

### 5.1 DESIGN PRINCIPLES

- Foreground Service is ALWAYS minimal — does only what's needed
- Network calls = rare, batched, WiFi-preferred
- Room = source of truth for all quiz and attempt data
- Supabase = backup + analysis layer, not real-time
- DataStore = fast lightweight flags, NOT quiz data

---

### 5.2 FOREGROUND SERVICE — OverlayForegroundService

```kotlin
class OverlayForegroundService : Service() {

    // WHAT THIS SERVICE DOES:
    // 1. Shows persistent notification (required by Android for foreground)
    // 2. Runs a polling loop (coroutine) every 30 seconds
    // 3. Calls TriggerEngine.checkAndFire() on each tick
    // 4. If TriggerDecision.Fire: draws overlay via WindowManager
    // 5. Listens for QuizAttemptResult and writes to Room
    // 6. Updates DataStore trigger state (lastQuizShownTime, etc.)

    // WHAT THIS SERVICE DOES NOT DO:
    // - No network calls (that is the SyncWorker's job)
    // - No heavy Room queries on main thread (use IO dispatcher)
    // - No GPS or other sensors
    // - No audio or camera

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        startPollingLoop()
        return START_STICKY  // restart if killed, important for overlay continuity
    }

    private fun startPollingLoop() {
        pollingJob = serviceScope.launch {
            while (isActive) {
                val decision = triggerEngine.checkAndFire()
                if (decision is TriggerDecision.Fire) {
                    withContext(Dispatchers.Main) {
                        showOverlay(decision.question)
                    }
                }
                delay(TriggerConfig.POLLING_INTERVAL_MS)
            }
        }
    }

    private fun showOverlay(question: QuizQuestion) {
        // 1. Inflate ComposeView with QuizCardRouter
        // 2. Add to WindowManager with params from Section 1.4
        // 3. Set onResult callback → onQuizResult()
        // 4. Update DataStore: overlayActive = true
    }

    private fun onQuizResult(result: QuizAttemptResult) {
        serviceScope.launch(Dispatchers.IO) {
            // 1. Remove overlay view from WindowManager (main thread)
            // 2. Write result to Room quiz_attempts table
            // 3. Write event log to Room event_logs table
            // 4. Update DataStore trigger state
            // 5. Mark result as unsynced (synced = false in Room row)
            // Note: DO NOT call Supabase here. That is SyncWorker's job.
        }
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        removeOverlayIfShowing()
        super.onDestroy()
    }
}
```

Notification (required for foreground service):
```kotlin
fun buildNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Learning mode active")
        .setContentText("Quiz overlay is running")
        .setSmallIcon(R.drawable.ic_brain)
        .setPriority(NotificationCompat.PRIORITY_LOW)  // LOW = no sound, minimal UI
        .setOngoing(true)
        .build()
}
// Use PRIORITY_LOW — HIGH or DEFAULT would alert the child on every quiz
```

---

### 5.3 BACKGROUND SYNC — SyncWorker

```kotlin
class SyncWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    // WHAT THIS WORKER DOES:
    // 1. Reads all unsynced quiz_attempts from Room (synced = false)
    // 2. Reads all unsynced event_logs from Room (synced = false)
    // 3. Batch POSTs both to Supabase REST API via Retrofit
    // 4. On success: marks rows as synced = true in Room
    // 5. On failure: logs error, leaves rows unsynced (will retry next run)

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val unsyncedAttempts = roomDao.getUnsyncedAttempts()
                val unsyncedEvents = roomDao.getUnsyncedEvents()

                if (unsyncedAttempts.isEmpty() && unsyncedEvents.isEmpty())
                    return@withContext Result.success()

                // Batch upsert to Supabase
                supabaseApi.batchInsertAttempts(unsyncedAttempts.map { it.toDto() })
                supabaseApi.batchInsertEvents(unsyncedEvents.map { it.toDto() })

                // Mark synced in Room
                roomDao.markAttemptsSynced(unsyncedAttempts.map { it.id })
                roomDao.markEventsSynced(unsyncedEvents.map { it.id })

                Result.success()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Result.retry()  // WorkManager will retry with backoff
            }
        }
    }
}
```

SyncWorker scheduling:
```kotlin
fun scheduleSyncWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // any network, not WiFi-only
        // WiFi-only would block sync on mobile data → bad for testers
        .build()

    val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
        repeatInterval = 30, TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "quiz_sync",
        ExistingPeriodicWorkPolicy.KEEP,  // don't reset if already scheduled
        syncRequest
    )
}
```

---

### 5.4 QUIZ QUESTION DOWNLOAD — QuizFetchWorker

```kotlin
class QuizFetchWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    // WHAT THIS WORKER DOES:
    // 1. Checks how many active questions are in Room
    // 2. If count >= MIN_CACHE_SIZE: skip (cache is healthy)
    // 3. If count < MIN_CACHE_SIZE: fetch from Supabase
    // 4. Upsert fetched questions into Room (INSERT OR REPLACE)
    // 5. Runs once on app open + once daily

    companion object {
        val MIN_CACHE_SIZE = 30   // always have at least 30 questions cached
        val MAX_FETCH = 100       // never fetch more than 100 at a time
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val cachedCount = roomDao.getActiveQuestionCount()
                if (cachedCount >= MIN_CACHE_SIZE) return@withContext Result.success()

                val questions = supabaseApi.fetchActiveQuestions(limit = MAX_FETCH)
                roomDao.upsertQuestions(questions.map { it.toEntity() })

                Result.success()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Result.retry()
            }
        }
    }
}
```

Trigger QuizFetchWorker:
```kotlin
// On app open (MainActivity.onCreate):
WorkManager.getInstance(this).enqueue(
    OneTimeWorkRequestBuilder<QuizFetchWorker>()
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build())
        .build()
)

// Also daily:
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "quiz_fetch",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<QuizFetchWorker>(24, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build())
        .build()
)
```

---

### 5.5 ROOM DATABASE — TABLE DEFINITIONS

```kotlin
@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val cardType: String,           // TAP_CHOICE | TAP_TAP_MATCH | DRAG_DROP_MATCH | FILL_BLANK
    val subject: String,            // math | english | science | general
    val difficulty: Int,            // 1 | 2 | 3
    val questionText: String,
    val instructionLabel: String,
    val mediaUrl: String?,
    val payloadJson: String,        // full payload as JSON string (parsed at render time)
    val timerSeconds: Int,
    val strictMode: Boolean,
    val active: Boolean,
    val fetchedAt: Long             // epoch ms — for cache freshness check
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,     // UUID generated locally
    val testerId: String,
    val questionId: String,
    val shownAt: Long,
    val answeredAt: Long?,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val sourceApp: String,
    val dismissed: Boolean,
    val synced: Boolean = false     // false until SyncWorker pushes to Supabase
)

@Entity(tableName = "event_logs")
data class EventLogEntity(
    @PrimaryKey val id: String,
    val testerId: String,
    val eventType: String,          // quiz_shown | quiz_answered | quiz_dismissed |
                                    // overlay_started | overlay_stopped | app_opened
    val payloadJson: String,
    val createdAt: Long,
    val synced: Boolean = false
)
```

---

### 5.6 DATA FLOW SUMMARY

```
QUIZ QUESTION FLOW (down):
Supabase quiz_questions
    ↓  QuizFetchWorker (on open + daily, network-gated)
Room quiz_questions (local cache, MIN 30 questions)
    ↓  TriggerEngine.selectNextQuestion()
OverlayForegroundService → WindowManager → Quiz Card UI

ATTEMPT DATA FLOW (up):
Quiz Card UI → onResult callback
    ↓  OverlayForegroundService (immediate, no network)
Room quiz_attempts (synced = false)
Room event_logs (synced = false)
    ↓  SyncWorker (every 30 min, network-gated, batched)
Supabase quiz_attempts
Supabase event_logs

NETWORK BUDGET PER SESSION:
  Quiz fetch   : ~50KB  (100 questions × ~500 bytes JSON)
  Attempt sync : ~2KB   (6 attempts × ~300 bytes each)
  Event sync   : ~3KB   (20 events × ~150 bytes each)
  Total        : ~55KB per session — extremely low
```

---

### 5.7 WHAT NEVER GOES OVER THE NETWORK IN MVP

```
NEVER sent to Supabase:
  - Child's name or any PII
  - Device ID or IMEI
  - Location data
  - Screen recordings or screenshots
  - Content of what the child was viewing in Instagram

ONLY sent to Supabase:
  - tester_id (anonymous UUID generated at install)
  - quiz attempt results (question_id, selected_option, correct, time)
  - event type logs (overlay_started, quiz_shown, etc.)
  - app version + overlay_permission_granted flag
```

---

---

# APPENDIX: FUNCTION DEPENDENCY MAP

```
MainActivity
  └── AppEntryRouter
        ├── OnboardingScreen
        ├── PinSetupScreen
        └── ChildHomeScreen
              └── "Parent" link → PinGateDialog → ParentDashboardScreen
                                                        ├── TodaySummaryCard    ← Room DAO
                                                        ├── WeekBarCard         ← Room DAO
                                                        ├── SubjectBreakdownCard← Room DAO
                                                        ├── RecentAttemptsCard  ← Room DAO
                                                        └── ActionButtons
                                                              └── PinGateDialog → disable overlay

OverlayForegroundService
  ├── TriggerEngine.checkAndFire()    (every 30s, Dispatchers.Default)
  │     ├── DataStore (read trigger state)
  │     └── Room DAO (getAttemptedIdsToday, getAllActive)
  ├── WindowManager.addView(QuizCardRouter)
  │     └── QuizCardRouter → [TapChoiceCard | TapTapMatchCard | DragDropMatchCard | FillBlankCard]
  └── onQuizResult()
        ├── Room DAO (insert quiz_attempts, insert event_logs)
        └── DataStore (update trigger state)

WorkManager
  ├── QuizFetchWorker (OneTime on open + Daily)
  │     ├── Room DAO (getActiveQuestionCount, upsertQuestions)
  │     └── Retrofit → Supabase REST (GET quiz_questions)
  └── SyncWorker (Every 30 min, network-gated)
        ├── Room DAO (getUnsyncedAttempts, getUnsyncedEvents, markSynced)
        └── Retrofit → Supabase REST (POST quiz_attempts, POST event_logs)
```

---

---

---

# SECTION 6: BLOCKER MITIGATIONS
## Technical Avoidance Patterns — All 10 Blockers

---

### BLOCKER 1: Permission Friction
**Risk:** Parent denies overlay / usage access / notifications at OS prompt.
**Gap in previous sections:** Onboarding was mentioned but not defined.

**Mitigation — Onboarding Permission Flow:**

```
SCREEN 1: Welcome
  "This app helps your child learn while they scroll."
  "We need 3 permissions to work."
  [ Let's set up → ]

SCREEN 2: Overlay Permission
  Title   : "Step 1 of 3 — Display over other apps"
  Body    : "Lets quiz cards appear on top of Instagram and YouTube."
  Button  : "Open Settings"  → deep link to Settings.ACTION_MANAGE_OVERLAY_PERMISSION
  After return: check Settings.canDrawOverlays(context)
  If granted: green checkmark, enable [ Next ] button
  If denied : show "This permission is required. Without it the app cannot work."
              [ Try Again ] button — re-opens settings

SCREEN 3: Usage Access
  Title   : "Step 2 of 3 — Usage access"
  Body    : "Lets the app know when Instagram or YouTube is open,
             so quizzes only appear at the right moment."
  Button  : "Open Settings" → Settings.ACTION_USAGE_ACCESS_SETTINGS
  After return: check AppOpsManager OPSTR_GET_USAGE_STATS
  Same granted/denied handling as Screen 2

SCREEN 4: Notifications
  Title   : "Step 3 of 3 — Notifications"
  Body    : "Required to keep the quiz service running in the background."
  Button  : "Allow"  → NotificationManagerCompat.requestNotificationPermission()
            (Android 13+ only; auto-granted below API 33)

SCREEN 5: Battery Optimization (see Blocker 6)
  Handled here in same onboarding flow.
```

```kotlin
// Permission check utilities
fun hasOverlayPermission(context: Context): Boolean =
    Settings.canDrawOverlays(context)

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
```

---

### BLOCKER 2: Accessibility Service Risk
**Risk:** Using AccessibilityService to monitor app content triggers Play Store rejection.
**Mitigation:** AccessibilityService is fully excluded from this MVP.
No AccessibilityService declaration in AndroidManifest.xml.
Use PACKAGE_USAGE_STATS instead (see Blocker 3).
✅ Already handled — no further code needed.

---

### BLOCKER 3: Detecting Instagram / YouTube Open
**Risk:** Without knowing which app is in foreground, overlay fires at wrong time.
**Mitigation:** Use UsageStatsManager — requires PACKAGE_USAGE_STATS permission
(granted via Settings, not a dangerous runtime permission).

```kotlin
object ForegroundAppDetector {

    // Target apps for MVP — hardcoded, no dynamic detection needed
    val TARGET_PACKAGES = setOf(
        "com.instagram.android",
        "com.google.android.youtube"
    )

    fun getForegroundPackage(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as UsageStatsManager
        val now = System.currentTimeMillis()
        // Query last 3 seconds — short window to get current foreground app
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 3000,
            now
        )
        return stats
            ?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    fun isTargetAppInForeground(context: Context): Boolean {
        return getForegroundPackage(context) in TARGET_PACKAGES
    }
}
```

Integration in OverlayForegroundService polling loop:
```kotlin
// Add this as Gate 0 — before all other trigger gates
private fun startPollingLoop() {
    pollingJob = serviceScope.launch {
        while (isActive) {
            val foreground = ForegroundAppDetector.getForegroundPackage(applicationContext)
            val isTarget = foreground in ForegroundAppDetector.TARGET_PACKAGES

            if (isTarget) {
                // Update session start time if this is a fresh session
                prefs.updateSessionIfNeeded(foreground!!)
                // Now check trigger gates
                val decision = triggerEngine.checkAndFire()
                if (decision is TriggerDecision.Fire) {
                    withContext(Dispatchers.Main) { showOverlay(decision.question) }
                }
            } else {
                // Not in target app — reset session tracking
                prefs.clearActiveSession()
            }
            delay(TriggerConfig.POLLING_INTERVAL_MS)
        }
    }
}
```

DataStore session tracking:
```kotlin
// When target app enters foreground and no session is active:
suspend fun updateSessionIfNeeded(packageName: String) {
    val current = prefs.getActiveSessionPackage()
    if (current != packageName) {
        // New session started
        prefs.setSessionStart(System.currentTimeMillis())
        prefs.setActiveSessionPackage(packageName)
    }
}

// When non-target app is in foreground:
suspend fun clearActiveSession() {
    prefs.clearSessionStart()
    prefs.clearActiveSessionPackage()
}
```

---

### BLOCKER 4: Detecting Reels / Video Playing
**Risk:** No official API to detect exact Reels or Shorts playback.
**Mitigation:** Three-signal heuristic — all three must be true to confirm
"child is watching video content." No single signal is reliable alone.

```kotlin
object VideoPlaybackDetector {

    // Signal 1: Target app is in foreground (from Blocker 3)
    // Signal 2: Audio is playing (MediaSession or AudioManager)
    // Signal 3: Session has been active for WARMUP_MS (not a cold open)

    fun isVideoLikelyPlaying(context: Context): Boolean {
        val signal1 = ForegroundAppDetector.isTargetAppInForeground(context)
        val signal2 = isAudioPlaying(context)
        val signal3 = true // checked via DataStore session start time in TriggerEngine

        // Require signal1 + at least one of signal2/signal3
        // This avoids false negatives when audio is muted
        return signal1 && (signal2 || signal3)
    }

    // AudioManager music stream active check
    private fun isAudioPlaying(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isMusicActive
    }
}
```

Why this is good enough for MVP:
- We don't need to know the exact video — we need to know the child is engaged
- "In Instagram + audio playing + 3 min in session" = high confidence of Reels viewing
- False positives (child reading feed, not watching) still produce valid quiz data
- False negatives (video muted) are caught by session timer fallback

MediaSession listener (optional enhancement, add if signal2 proves unreliable):
```kotlin
// Register in OverlayForegroundService.onCreate()
val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE)
    as MediaSessionManager
// Requires MEDIA_CONTENT_CONTROL permission — NOT needed for MVP
// AudioManager.isMusicActive() is sufficient and permission-free
```

---

### BLOCKER 5: Auto Pause / Fake Pause
**Risk:** Cannot call Instagram's pause API. Child continues watching under quiz.
**Mitigation:** Full-screen opaque overlay + system audio mute = "fake pause."
This requires DIFFERENT WindowManager flags than Section 1.4.

**Two overlay modes — choose based on quiz card config `strict_mode`:**

```kotlin
// MODE A: Non-strict (default) — partial overlay, touch passthrough allowed
// Used for: TAP_CHOICE, FILL_BLANK
// Section 1.4 flags apply — FLAG_NOT_FOCUSABLE, 56% height
// Child can see top of Instagram. No fake pause.

// MODE B: Strict mode — full screen opaque overlay + audio mute
// Used for: DRAG_DROP_MATCH, TAP_TAP_MATCH (longer interactions)
// OR when rules.strict_mode == true in quiz config

fun getWindowParamsForMode(screenHeight: Int, strictMode: Boolean): WindowManager.LayoutParams {
    return if (strictMode) {
        // STRICT: full screen, consumes ALL touches, no passthrough
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,   // full screen
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            // Note: FLAG_NOT_FOCUSABLE is REMOVED for strict mode
            // This makes the overlay consume all touch events
            PixelFormat.OPAQUE   // fully opaque — child cannot see Instagram
        ).apply { gravity = Gravity.TOP }
    } else {
        // NON-STRICT: bottom sheet, passes touches through transparent top area
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (screenHeight * 0.56).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
    }
}
```

Audio mute for strict mode fake pause:
```kotlin
fun muteSystemAudio(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        AudioManager.ADJUST_MUTE,
        0  // no UI flag — silent mute
    )
}

fun restoreSystemAudio(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        AudioManager.ADJUST_UNMUTE,
        0
    )
}

// Call muteSystemAudio() when strict overlay appears
// Call restoreSystemAudio() in onQuizResult() before removing overlay
```

**Important:** Always restore audio in onDestroy() of OverlayForegroundService
as a safety net in case the service is killed before quiz completes.

---

### BLOCKER 6: OEM Battery Kill (Xiaomi / Oppo / Vivo / Samsung)
**Risk:** Aggressive OEM battery optimization kills Foreground Service silently.
**Mitigation:** Three-layer defense.

**Layer 1 — Onboarding Battery Optimization Screen:**
```kotlin
// Add as Screen 5 in onboarding flow (after permissions)

@Composable
fun BatteryOptimizationScreen(onContinue: () -> Unit) {
    // Title: "One last step — keep quizzes running"
    // Body:  "Some phones stop background apps to save battery.
    //         Disable this for our app so quizzes never miss a moment."
    // Button: "Disable battery optimization"
    //         → opens ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS intent
    // After return: check isIgnoringBatteryOptimizations()
    // If granted: green check, show [ Continue ]
    // If denied:  yellow warning "Quizzes may stop on some phones"
    //             still allow [ Continue ] — don't block onboarding
}

fun requestIgnoreBatteryOptimization(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

fun isIgnoringBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
```

**Layer 2 — START_STICKY + Restart Receiver:**
```kotlin
// Already in OverlayForegroundService:
override fun onStartCommand(...): Int = START_STICKY

// Add BroadcastReceiver to restart service if killed:
class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.yourapp.RESTART_SERVICE") {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OverlayForegroundService::class.java)
            )
        }
    }
}

// In OverlayForegroundService.onDestroy():
override fun onDestroy() {
    // Send restart broadcast with 2s delay
    val restartIntent = Intent("com.yourapp.RESTART_SERVICE")
    sendBroadcast(restartIntent)
    super.onDestroy()
}
```

**Layer 3 — WorkManager Watchdog:**
```kotlin
class ServiceWatchdogWorker(ctx: Context, params: WorkerParameters)
    : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val manager = ActivityManager::class.java
            .getMethod("getRunningServices", Int::class.java)
        val isRunning = /* check if OverlayForegroundService is in running services list */
            isServiceRunning(applicationContext, OverlayForegroundService::class.java)

        if (!isRunning && isOverlayEnabled(applicationContext)) {
            ContextCompat.startForegroundService(
                applicationContext,
                Intent(applicationContext, OverlayForegroundService::class.java)
            )
        }
        return Result.success()
    }
}

// Schedule: every 15 minutes (minimum WorkManager interval)
PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(15, TimeUnit.MINUTES).build()
```

---

### BLOCKER 7: Overlay Touch / Gesture Issues
**Risk:** Drag gestures pass through overlay to Instagram (swipe to next Reel).
**Mitigation:** Touch handling depends on overlay mode.

**For non-strict (partial overlay):**
```kotlin
// FLAG_NOT_FOCUSABLE allows touches through the TRANSPARENT TOP AREA only
// The card's own ComposeView handles touches inside its bounds normally
// Drag-drop quiz (TYPE 3) should NOT be used in non-strict mode
// because drag gestures near card edges may leak to Instagram

// Rule: DRAG_DROP_MATCH card type MUST always use strict_mode = true
// Enforce this in QuizCardRouter:
fun QuizCardRouter(...) {
    val effectiveConfig = if (config.cardType == QuizCardType.DRAG_DROP_MATCH) {
        config.copy(rules = config.rules.copy(strictMode = true))
    } else config

    // render with effectiveConfig
}
```

**For strict (full-screen overlay):**
```kotlin
// FLAG_NOT_TOUCH_MODAL without FLAG_NOT_FOCUSABLE = card consumes ALL touches
// No gesture leaks to Instagram. Child CANNOT swipe to next Reel.
// This is correct and intentional for strict mode.

// Additional: intercept back gesture
// Add to ComposeView in strict mode:
BackHandler(enabled = strictMode) {
    // Do nothing — prevent back press from dismissing overlay in strict mode
}
```

---

### BLOCKER 8: Play Store "Interfering with Other Apps" Risk
**Risk:** Play Store reviewer sees overlay on Instagram and flags as disruptive.
**Mitigation:** Positioning, manifest, and store listing strategy.

App Store listing framing:
```
Category    : Education > Tools for Families
Title       : [AppName] — Learning Overlay for Kids
Description : A parental tool that shows educational quiz cards while
              children use social media. Parent-controlled, opt-in only.
              Requires explicit setup by a parent or guardian.
```

AndroidManifest.xml — declare purpose clearly:
```xml
<!-- Declare targeted use of overlay permission -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions"/>

<!-- Tag app as family-safe -->
<application
    android:label="@string/app_name"
    ...>
    <!-- No android:isGame="true" -->
    <!-- Target families policy compliance metadata -->
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
        <!-- Only include if using AdMob; for MVP with no ads, omit entirely -->
</application>
```

Play Store review defense checklist:
```
✓ Overlay only appears when parent has enabled it (opt-in)
✓ Overlay only appears on explicitly listed target apps
✓ Child cannot be harmed — no data collection, no purchases
✓ Parental PIN required to change any settings
✓ Onboarding clearly explains what the app does before any permission
✓ App does NOT inject code into or modify Instagram/YouTube
✓ App does NOT read Instagram/YouTube content (only detects foreground)
✓ Persistent notification makes overlay visible and stoppable at all times
```

For MVP (APK sideload to 10 testers):
Play Store review is not relevant yet. Apply these rules before v1 public launch.

---

### BLOCKER 9: MediaProjection / Screen Capture Risk
**Risk:** Using MediaProjection to screenshot or record Instagram triggers Play review
         and requires a foreground notification with capture icon.
**Mitigation:** MediaProjection is fully excluded. Not referenced anywhere in this MVP.
The "fake pause" effect (Blocker 5) uses audio mute + opaque overlay instead.
✅ Already handled — no further code needed.

---

### BLOCKER 10: Child Compliance — COPPA / GDPR-K
**Risk:** App used by children under 13 triggers COPPA (US) and GDPR-K (EU) requirements:
         parental consent, no behavioral advertising, no persistent identifiers.
**Mitigation:** Minimal data design + parent-controlled consent.

Data minimization rules (extends Section 5.7):
```
COLLECTED AND STORED LOCALLY (Room):
  - Anonymous tester_id (UUID, generated at install, no name/email)
  - Quiz answers (question_id, correct/wrong, response time)
  - Session events (overlay_started, quiz_shown)

SENT TO SUPABASE:
  - Same as above — no PII

NEVER COLLECTED:
  - Child's name, age, or photo
  - Device IMEI, MAC address, advertising ID
  - Location
  - Contact list
  - Instagram/YouTube account info or content

ADVERTISING ID — DISABLE EXPLICITLY:
```
```kotlin
// In Application.onCreate():
AdvertisingIdClient.getAdvertisingIdInfo(context) // DO NOT call this
// Instead, opt out at manifest level:
```
```xml
<!-- AndroidManifest.xml -->
<meta-data
    android:name="com.google.android.gms.ads.AD_MANAGER_APP"
    android:value="false"/>
```

Parental consent screen (add to onboarding after PIN setup):
```kotlin
@Composable
fun ParentalConsentScreen(onAccepted: () -> Unit, onDeclined: () -> Unit) {
    // Title: "Parent or Guardian Consent"
    // Body (plain language):
    //   "This app collects anonymous usage data (quiz answers and timing)
    //    to help us improve learning outcomes. No personal information
    //    about your child is collected or shared.
    //    You are setting this up for a child under your supervision."
    // Checkbox: "I am the parent or guardian and I consent"
    // [ Continue ] button — enabled only when checkbox ticked
    // [ Decline ] — exits app
    // Save consent timestamp to DataStore
}
```

```kotlin
// DataStore keys for compliance
object ConsentKeys {
    val CONSENT_GIVEN = booleanPreferencesKey("parental_consent_given")
    val CONSENT_TIMESTAMP = longPreferencesKey("consent_given_at")
    val CONSENT_VERSION = intPreferencesKey("consent_version") // bump when policy changes
}
```

---

### 6.1 REVISED APP ENTRY ROUTING (includes all blocker mitigations)

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

*End of MVP Functions Definition — R2*
*Sections 1–5: What to build. Section 6: What to avoid and how.*
*Build order: Permission flow → ForegroundAppDetector → OverlayService → TriggerEngine → Quiz UI → Dashboard*