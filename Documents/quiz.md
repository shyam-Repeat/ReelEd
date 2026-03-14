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
