# SECTION 1: QUIZ CARD OVERLAY UI
## Configuration-Driven / Data-Driven Architecture — R3

---

### 1.1 CORE DESIGN PRINCIPLE

The quiz card is 100% driven by a QuizCardConfig object.
The UI does NOT hard-code any quiz type. Instead:

- Supabase sends a question with a `card_type` field
- Room caches it as a raw JSON string (payloadJson)
- On display, payloadJson is parsed into a typed QuizCardConfig
- The router reads `card_type` and renders the matching Composable
- New quiz types = new JSON + new Composable, no other changes

---

### 1.2 QUIZ CARD CONFIG — MASTER JSON SCHEMA

Every question row in Supabase and Room carries this shape:
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
    "show_correct_on_wrong": true
  }
}
```

Field definitions:

| Field                     | Type     | Purpose                              |
|---------------------------|----------|--------------------------------------|
| card_type                 | String   | Which Composable to render           |
| display.question_text     | String   | Main question shown to child         |
| display.instruction_label | String   | Sub-label ("Tap", "Drag", etc.)      |
| display.media_url         | String?  | Optional image/icon URL              |
| payload                   | Object   | Type-specific data — parsed at render|
| rules.timer_seconds       | Int      | 0 = no timer                         |
| rules.strict_mode         | Boolean  | true = no dismiss, full-screen overlay|
| rules.show_correct_on_wrong | Boolean | Reveal correct answer after wrong tap|

NOTE: max_attempts removed. All MVP cards allow exactly 1 attempt.
Enforced in Composable state (buttons locked after first tap), not config.

---

### 1.3 CARD TYPE ENUM — 4 FIXED TYPES
```kotlin
enum class QuizCardType {
    TAP_CHOICE,      // 4-option MCQ tap
    TAP_TAP_MATCH,   // Two columns, tap left then tap right to pair
    DRAG_DROP_MATCH, // Drag chip from pool, drop onto labeled slot
    FILL_BLANK       // Tap word from bank to fill sentence blank
}
```

---

### 1.4 KOTLIN DATA CLASSES — TYPED CONFIG LAYER
#### FIX for Issue 1 + Issue 4

Room stores payloadJson as a raw String. Before rendering,
call QuizCardConfig.from(entity) to parse it into typed objects.
Composables receive QuizCardConfig — never the raw String.
```kotlin
// ── Payload sealed hierarchy ──────────────────────────────────────

sealed class QuizPayload {

    data class TapChoicePayload(
        val options: List<ChoiceOption>
    ) : QuizPayload()

    data class TapTapMatchPayload(
        val pairs: List<MatchPair>,
        val rightOrderShuffled: List<String>   // pre-shuffled right IDs
    ) : QuizPayload()

    data class DragDropPayload(
        val chips: List<DragChip>,
        val slots: List<DropSlot>
    ) : QuizPayload()

    data class FillBlankPayload(
        val sentenceTemplate: String,           // contains "___" as blank marker
        val wordBank: List<WordChip>
    ) : QuizPayload()
}

data class ChoiceOption(val id: String, val label: String, val isCorrect: Boolean)
data class MatchPair(val leftId: String, val leftLabel: String,
                     val rightId: String, val rightLabel: String)
data class DragChip(val chipId: String, val label: String)
data class DropSlot(val slotId: String, val slotLabel: String, val correctChipIds: List<String>)
data class WordChip(val chipId: String, val label: String, val isCorrect: Boolean)

// ── Display + Rules ───────────────────────────────────────────────

data class QuizDisplay(
    val questionText: String,
    val instructionLabel: String,
    val mediaUrl: String?
)

data class QuizRules(
    val timerSeconds: Int,        // 0 = no timer
    val strictMode: Boolean,      // true = full-screen opaque, no dismiss
    val showCorrectOnWrong: Boolean
)

// ── Master config ─────────────────────────────────────────────────

data class QuizCardConfig(
    val id: String,
    val cardType: QuizCardType,
    val subject: String,
    val difficulty: Int,
    val display: QuizDisplay,
    val payload: QuizPayload,
    val rules: QuizRules
) {
    companion object {
        // Call this in ViewModel or Repository — never inside a Composable
        fun from(entity: QuizQuestionEntity): QuizCardConfig {
            val type = QuizCardType.valueOf(entity.cardType)
            val payload = parsePayload(type, entity.payloadJson)
            return QuizCardConfig(
                id              = entity.id,
                cardType        = type,
                subject         = entity.subject,
                difficulty      = entity.difficulty,
                display         = QuizDisplay(
                    questionText     = entity.questionText,
                    instructionLabel = entity.instructionLabel,
                    mediaUrl         = entity.mediaUrl
                ),
                payload         = payload,
                rules           = QuizRules(
                    timerSeconds      = entity.timerSeconds,
                    strictMode        = entity.strictMode,
                    showCorrectOnWrong = entity.showCorrectOnWrong
                )
            )
        }

        private fun parsePayload(type: QuizCardType, json: String): QuizPayload {
            val root = JSONObject(json)
            return when (type) {
                QuizCardType.TAP_CHOICE -> {
                    val options = root.getJSONArray("options")
                    QuizPayload.TapChoicePayload(
                        options = List(options.length()) { index ->
                            val item = options.getJSONObject(index)
                            ChoiceOption(
                                id = item.getString("id"),
                                label = item.getString("label"),
                                isCorrect = item.getBoolean("is_correct"),
                                color = item.optString("color", null)
                            )
                        }
                    )
                }

                QuizCardType.TAP_TAP_MATCH -> {
                    val leftItems = root.getJSONArray("left_items")
                    val rightItems = root.getJSONArray("right_items")
                    val correctPairs = root.getJSONArray("correct_pairs")

                    val rightLabelMap = buildMap {
                        repeat(rightItems.length()) { i ->
                            val item = rightItems.getJSONObject(i)
                            put(item.getString("id"), item.getString("label"))
                        }
                    }
                    val leftLabelMap = buildMap {
                        repeat(leftItems.length()) { i ->
                            val item = leftItems.getJSONObject(i)
                            put(item.getString("id"), item.getString("label"))
                        }
                    }

                    val pairs = List(correctPairs.length()) { index ->
                        val pair = correctPairs.getJSONArray(index)
                        val leftId = pair.getString(0)
                        val rightId = pair.getString(1)
                        MatchPair(
                            leftId = leftId,
                            leftLabel = leftLabelMap[leftId] ?: "",
                            rightId = rightId,
                            rightLabel = rightLabelMap[rightId] ?: ""
                        )
                    }

                    val rightOrderShuffled = List(rightItems.length()) { i ->
                        rightItems.getJSONObject(i).getString("id")
                    }

                    QuizPayload.TapTapMatchPayload(pairs, rightOrderShuffled)
                }

                QuizCardType.DRAG_DROP_MATCH -> {
                    val draggables = root.getJSONArray("draggables")
                    val targets = root.getJSONArray("targets")
                    val correctPairs = root.getJSONArray("correct_pairs")
                    val correctPairMap = mutableMapOf<String, MutableList<String>>()

                    repeat(correctPairs.length()) { index ->
                        val pair = correctPairs.getJSONArray(index)
                        val dragId = pair.getString(0)
                        val targetId = pair.getString(1)
                        correctPairMap.getOrPut(targetId) { mutableListOf() }.add(dragId)
                    }

                    QuizPayload.DragDropPayload(
                        draggables = List(draggables.length()) { index ->
                            val item = draggables.getJSONObject(index)
                            DragChip(item.getString("id"), item.getString("label"))
                        },
                        targets = List(targets.length()) { index ->
                            val item = targets.getJSONObject(index)
                            val id = item.getString("id")
                            DropSlot(id, item.getString("label"), correctPairMap[id].orEmpty())
                        }
                    )
                }

                QuizCardType.FILL_BLANK -> {
                    val wordBank = root.getJSONArray("word_bank")
                    QuizPayload.FillBlankPayload(
                        sentenceTemplate = root.getString("sentence_template"),
                        wordBank = List(wordBank.length()) { index ->
                            val item = wordBank.getJSONObject(index)
                            WordChip(
                                chipId = item.getString("chip_id"),
                                label = item.getString("label"),
                                isCorrect = item.optBoolean("is_correct", false)
                            )
                        }
                    )
                }
            }
        }
    }
}
```

---

### 1.5 RESULT TYPE — UNIFIED ACROSS ALL CARD TYPES
```kotlin
// Single result type flowing from every card type back to the Service.
// Fixes Issue 2 — all Composables use this, no mismatched lambda types.

data class QuizAttemptResult(
    val questionId: String,
    val selectedOptionId: String?,   // null if timer expired or dismissed
    val isCorrect: Boolean,
    val wasDismissed: Boolean,
    val wasTimerExpired: Boolean,
    val responseTimeMs: Long,
    val sourceApp: String            // e.g. "com.instagram.android"
)
```

---

### 1.6 CARD SIZE & OVERLAY CONSTRAINTS

Two overlay modes driven by `rules.strict_mode`.
Mode B (strict) is always used for DRAG_DROP_MATCH regardless of config value.
```
NON-STRICT MODE (TAP_CHOICE, FILL_BLANK, TAP_TAP_MATCH)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Position   : Bottom-anchored
Width      : 100% screen width
Height     : 56% of screen height
             (~44% of Instagram/YouTube stays visible above)
Background : Solid white / solid dark, no blur
Corner Rx  : 20dp top corners only
Pixel fmt  : TRANSLUCENT (transparent region above card passes
             touches through to Instagram — child can still see feed)
Flags      : FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN
Animation  : Slide up 280ms ease-out / slide down 200ms ease-in
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

STRICT MODE (DRAG_DROP_MATCH, or strict_mode = true in config)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Position   : Full screen
Width      : 100% screen width
Height     : 100% screen height (MATCH_PARENT)
Background : Solid opaque — child cannot see Instagram beneath
Pixel fmt  : OPAQUE
Flags      : FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_IN_SCREEN
             (FLAG_NOT_FOCUSABLE intentionally REMOVED —
              card consumes ALL touches, nothing leaks to Instagram)
Audio      : Mute system audio on show, restore on dismiss
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
```kotlin
fun buildWindowParams(screenHeight: Int, strictMode: Boolean): WindowManager.LayoutParams {
    return if (strictMode) {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        ).apply { gravity = Gravity.TOP }
    } else {
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

---

### 1.7 COMPOSEVIEW IN WINDOWMANAGER — LIFECYCLE SETUP
#### FIX for Issue 3

A ComposeView inflated inside a Service has no Activity lifecycle.
Without attaching one manually, any LaunchedEffect, rememberCoroutineScope,
or state animation will throw IllegalStateException at runtime.

Do this ONCE when creating the overlay view, before windowManager.addView():
```kotlin
// Inside OverlayForegroundService — showOverlay() function

private fun showOverlay(config: QuizCardConfig, sourceApp: String) {
    val context = this

    // 1. Create the ComposeView
    val composeView = ComposeView(context)

    // 2. Attach a LifecycleOwner — REQUIRED for Compose in a Service
    val lifecycleOwner = OverlayLifecycleOwner()
    lifecycleOwner.performRestore(null)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    // 3. Attach to the view tree — these two lines are the critical fix
    ViewTreeLifecycleOwner.set(composeView, lifecycleOwner)
    ViewTreeSavedStateRegistryOwner.set(composeView, lifecycleOwner)

    // 4. Set Compose content — pass result callback (fixes Issue 6)
    composeView.setContent {
        QuizCardRouter(
            config    = config,
            sourceApp = sourceApp,
            onResult  = { result -> onQuizResult(result, lifecycleOwner) },
            onDismissed = { onQuizDismissed(config.id, sourceApp, lifecycleOwner) }
        )
    }

    // 5. Add to WindowManager
    val isStrict = config.rules.strictMode ||
                   config.cardType == QuizCardType.DRAG_DROP_MATCH
    val params = buildWindowParams(getScreenHeight(), isStrict)
    windowManager.addView(composeView, params)
    activeComposeView = composeView
    activeLifecycleOwner = lifecycleOwner

    if (isStrict) muteSystemAudio()
}

private fun removeOverlay(lifecycleOwner: OverlayLifecycleOwner) {
    activeComposeView?.let { windowManager.removeView(it) }
    activeComposeView = null
    // Tear down lifecycle cleanly
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    restoreSystemAudio()
}

// Minimal LifecycleOwner + SavedStateRegistryOwner implementation
// needed to host Compose outside an Activity

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) =
        lifecycleRegistry.handleLifecycleEvent(event)

    fun performRestore(savedState: Bundle?) =
        savedStateRegistryController.performRestore(savedState)
}
```

---

### 1.8 SERVICE → COMPOSABLE RESULT BRIDGE
#### FIX for Issue 6

The onResult and onDismissed lambdas are created in showOverlay()
inside the Service and passed directly into QuizCardRouter.
When the child answers, the Composable calls onResult() which
executes back in the Service context where Room writes happen.
No event bus or shared StateFlow needed — simple lambda closure.
```kotlin
// Inside OverlayForegroundService

private fun onQuizResult(result: QuizAttemptResult, lco: OverlayLifecycleOwner) {
    serviceScope.launch(Dispatchers.IO) {
        // 1. Remove overlay (main thread)
        withContext(Dispatchers.Main) { removeOverlay(lco) }
        // 2. Write to Room
        roomDao.insertAttempt(result.toEntity(testerId))
        roomDao.insertEvent(result.toEventLog(testerId))
        // 3. Update DataStore trigger state
        triggerPrefs.recordQuizShown(
            questionId  = result.questionId,
            wasDismissed = result.wasDismissed
        )
    }
}

private fun onQuizDismissed(questionId: String, sourceApp: String,
                            lco: OverlayLifecycleOwner) {
    onQuizResult(
        QuizAttemptResult(
            questionId      = questionId,
            selectedOptionId = null,
            isCorrect        = false,
            wasDismissed     = true,
            wasTimerExpired  = false,
            responseTimeMs   = 0L,
            sourceApp        = sourceApp
        ),
        lco
    )
}
```

---

### 1.9 COMPOSABLE FUNCTION DEFINITIONS — 4 CARD TYPES
#### All signatures now unified — FIX for Issue 2

All 4 card types share the same callback contract:
  onResult: (QuizAttemptResult) -> Unit
  onDismissed: () -> Unit

Timer expiry is handled INSIDE each Composable — it builds
a QuizAttemptResult with wasTimerExpired=true and calls onResult().
No separate onTimerExpired callback needed or used.

---

#### TYPE 1: TAP_CHOICE

Use case: Math facts, general knowledge, vocabulary
Best for: Quick interruption, under 20 seconds
Strict mode: false (bottom-sheet overlay)
```
VISUAL LAYOUT (top to bottom inside card)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ Timer bar — thin strip, drains left to right        ]
[ Question Text — 18sp bold, max 2 lines              ]
[ Instruction label — 12sp muted                      ]
────────────────────────────────────────────────────
[ Option A ]  [ Option B ]
[ Option C ]  [ Option D ]
────────────────────────────────────────────────────
[ Dismiss — tiny right-aligned, only if !strict_mode ]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
```kotlin
@Composable
fun TapChoiceCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val payload = config.payload as QuizPayload.TapChoicePayload
    val rules   = config.rules

    var selectedId   by remember { mutableStateOf<String?>(null) }
    var locked       by remember { mutableStateOf(false) }      // true after any tap or timer end
    var revealed     by remember { mutableStateOf(false) }
    val startTime    = remember { System.currentTimeMillis() }

    // Timer — only runs if rules.timerSeconds > 0
    // Uses LaunchedEffect(Unit) + countdown loop
    // On expire: build result with wasTimerExpired=true, call onResult()
    LaunchedEffect(Unit) {
        if (rules.timerSeconds > 0) {
            delay(rules.timerSeconds * 1000L)
            if (!locked) {
                locked = true
                onResult(
                    QuizAttemptResult(
                        questionId       = config.id,
                        selectedOptionId = null,
                        isCorrect        = false,
                        wasDismissed     = false,
                        wasTimerExpired  = true,
                        responseTimeMs   = rules.timerSeconds * 1000L,
                        sourceApp        = sourceApp
                    )
                )
            }
        }
    }

    // Option tap handler
    fun onOptionTap(option: ChoiceOption) {
        if (locked) return
        locked = true
        selectedId = option.id
        val elapsed = System.currentTimeMillis() - startTime
        val result = QuizAttemptResult(
            questionId       = config.id,
            selectedOptionId = option.id,
            isCorrect        = option.isCorrect,
            wasDismissed     = false,
            wasTimerExpired  = false,
            responseTimeMs   = elapsed,
            sourceApp        = sourceApp
        )
        // If wrong and show_correct_on_wrong: reveal for 1500ms then fire result
        // If correct: flash green for 800ms then fire result
        // Both paths end with onResult(result)
    }

    // Button color logic (each option):
    // Default    : outline border, white fill
    // Selected   : blue fill
    // Correct    : green fill (after reveal=true)
    // Wrong      : red fill (after reveal=true && id == selectedId)
}
```

Payload JSON:
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

#### TYPE 2: TAP_TAP_MATCH

Use case: Word-definition pairs, translation matching
Best for: Vocabulary, language learning, age 8+
Strict mode: false (bottom-sheet)
```
VISUAL LAYOUT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ Question Text                         ]
[ Instruction: "Match each word"        ]
────────────────────────────────────────────────────
LEFT COLUMN        RIGHT COLUMN (shuffled order)
[ Dog   ]          [ Move fast ]
[ Run   ]          [ Food      ]
[ Big   ]          [ Animal    ]
[ Eat   ]          [ Large     ]
────────────────────────────────────────────────────
Tap left item → highlights it
Tap right item → checks pair:
  Correct : green line drawn, both locked, cleared selection
  Wrong   : red flash on both, reset selection after 600ms
All pairs matched → auto-advance after 600ms
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Payload JSON:
```json
"payload": {
  "left_items": [
    { "id": "L1", "label": "Dog" },
    { "id": "L2", "label": "Run" },
    { "id": "L3", "label": "Big" },
    { "id": "L4", "label": "Eat" }
  ],
  "right_items": [
    { "id": "R1", "label": "Move fast" },
    { "id": "R2", "label": "Food" },
    { "id": "R3", "label": "Animal" },
    { "id": "R4", "label": "Large" }
  ],
  "correct_pairs": [
    ["L1", "R3"],
    ["L2", "R1"],
    ["L3", "R4"],
    ["L4", "R2"]
  ]
}
```

---

#### TYPE 3: DRAG_DROP_MATCH

Use case: Ordering steps, sorting numbers/words, categorising
Best for: Age 9+, longer engagement (30+ sec)
Strict mode: ALWAYS true — enforced in QuizCardRouter regardless of config.
             DRAG_DROP requires full-screen opaque overlay to prevent
             drag gestures leaking to Instagram underneath.
```
VISUAL LAYOUT (full screen — no Instagram visible beneath)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ Question: "Put these numbers in order" ]
────────────────────────────────────────
CHIP POOL (wrapping row, top area):
[ 7 ]  [ 2 ]  [ 9 ]  [ 4 ]

DROP SLOTS (below, labeled):
[ Smallest ___ ]  [ 2nd ___ ]
[ 3rd      ___ ]  [ Largest ___ ]

Chip dragged → follows finger immediately (no long-press)
Dropped on slot → chip sits inside slot
Tap filled slot → chip returns to pool
All slots filled → [ Submit ] appears
Submit → green correct / red wrong slots for 1500ms → onResult()
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Payload JSON:
```json
"payload": {
  "draggables": [
    { "id": "D1", "label": "2" },
    { "id": "D2", "label": "4" },
    { "id": "D3", "label": "7" },
    { "id": "D4", "label": "9" }
  ],
  "targets": [
    { "id": "T1", "label": "Smallest" },
    { "id": "T2", "label": "2nd" },
    { "id": "T3", "label": "3rd" },
    { "id": "T4", "label": "Largest" }
  ],
  "correct_pairs": [
    ["D1", "T1"],
    ["D2", "T2"],
    ["D3", "T3"],
    ["D4", "T4"]
  ]
}
```

---

#### TYPE 4: FILL_BLANK

Use case: Sentence completion, vocabulary, spelling
Best for: Language, English, early reading
Strict mode: false (bottom-sheet)
MVP: exactly 1 blank per sentence. Multi-blank is post-MVP.
```
VISUAL LAYOUT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ "The [ Sun ] is the closest star to Earth." ]
          ↑ filled blank shown inline

WORD BANK:
[ Moon ]  ~~[ Sun ]~~  [ Mars ]  [ Earth ]
          (Sun greyed out — already placed)

Tap word chip  → fills blank, grey out chip in bank
Tap filled blank → ejects word back to bank, re-enables chip
[ Submit ] → appears when blank is filled
Submit → green flash if correct, red + reveal if wrong → onResult()
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
```kotlin
@Composable
fun FillBlankCard(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    val payload   = config.payload as QuizPayload.FillBlankPayload
    val rules     = config.rules
    val startTime = remember { System.currentTimeMillis() }

    var blankFilledChipId by remember { mutableStateOf<String?>(null) }

    // Render sentence:
    // Split payload.sentenceTemplate on "___"
    // Part 0 → Text
    // Blank  → if blankFilledChipId == null: underlined empty Box (tappable to cancel)
    //          if filled: show chip label inline as highlighted text
    // Part 1 → Text

    // Word bank: Row of chips
    // Chip enabled = chipId != blankFilledChipId
    // On chip tap (when blank is empty): blankFilledChipId = chipId
    // On blank tap (when filled): blankFilledChipId = null

    // Submit appears when blankFilledChipId != null
    // On submit:
    //   val selected = payload.wordBank.find { it.chipId == blankFilledChipId }
    //   val isCorrect = selected?.isCorrect == true
    //   val elapsed   = System.currentTimeMillis() - startTime
    //   if correct: flash green → onResult(...)
    //   if wrong && showCorrectOnWrong: reveal correct chip → wait 1500ms → onResult(...)
    //   if wrong && !showCorrectOnWrong: flash red → onResult(...)
}
```

Payload JSON:
```json
"payload": {
  "sentence_template": "The ___ is the closest star to Earth.",
  "word_bank": [
    { "chip_id": "W1", "label": "Moon",  "is_correct": false },
    { "chip_id": "W2", "label": "Sun",   "is_correct": true  },
    { "chip_id": "W3", "label": "Mars",  "is_correct": false },
    { "chip_id": "W4", "label": "Earth", "is_correct": false }
  ]
}
```

Note: blank_position field removed. MVP has exactly 1 blank per sentence,
always marked by "___" in the template. No position index needed.

---

### 1.10 CARD TYPE ROUTER
#### FIX for Issue 2 — unified signature, strict enforcement for DRAG_DROP
```kotlin
@Composable
fun QuizCardRouter(
    config: QuizCardConfig,
    sourceApp: String,
    onResult: (QuizAttemptResult) -> Unit,
    onDismissed: () -> Unit
) {
    // DRAG_DROP always strict — override config value here, not in the Composable
    val effectiveConfig = if (config.cardType == QuizCardType.DRAG_DROP_MATCH) {
        config.copy(rules = config.rules.copy(strictMode = true))
    } else config

    when (effectiveConfig.cardType) {
        QuizCardType.TAP_CHOICE      -> TapChoiceCard(effectiveConfig, sourceApp, onResult, onDismissed)
        QuizCardType.TAP_TAP_MATCH   -> TapTapMatchCard(effectiveConfig, sourceApp, onResult, onDismissed)
        QuizCardType.DRAG_DROP_MATCH -> DragDropMatchCard(effectiveConfig, sourceApp, onResult, onDismissed)
        QuizCardType.FILL_BLANK      -> FillBlankCard(effectiveConfig, sourceApp, onResult, onDismissed)
    }
}
```

---

### 1.11 CARD STATE MACHINE
```
IDLE
  │
  ▼  showOverlay(config) called by OverlayForegroundService
     OverlayLifecycleOwner created + attached to ComposeView
     ComposeView added to WindowManager
APPEARING  ← slide-up animation 280ms (non-strict)
           ← fade-in 200ms (strict / full-screen)
  │
  ▼  animation complete
ACTIVE     ← child interacting, timer ticking (if timerSeconds > 0)
  │
  ├── child answers correctly
  │     → RESULT_FLASH_GREEN (800ms)
  │     → onResult(isCorrect=true, wasDismissed=false, wasTimerExpired=false)
  │
  ├── child answers incorrectly
  │     → RESULT_FLASH_RED + optional reveal (1500ms)
  │     → onResult(isCorrect=false, wasDismissed=false, wasTimerExpired=false)
  │
  ├── timer expires (timerSeconds > 0 only)
  │     → show correct answer briefly (1000ms)
  │     → onResult(isCorrect=false, wasDismissed=false, wasTimerExpired=true)
  │
  └── child taps Dismiss (only if !strict_mode)
        → onResult(isCorrect=false, wasDismissed=true, wasTimerExpired=false)
  │
  ▼  any onResult() or onDismissed() path
     Service calls removeOverlay()
     OverlayLifecycleOwner teardown (ON_PAUSE → ON_STOP → ON_DESTROY)
     ComposeView removed from WindowManager
     Audio restored (strict mode only)
DISAPPEARING  ← slide-down 200ms
  │
  ▼
IDLE  — TriggerEngine resumes polling, DataStore trigger state updated
```

---

*End of Section 1 — R3*
*Issues fixed: data class bridge (1), router signature (2), lifecycle crash (3),*
*payload parsing (4), drag gesture + flag conflict (5), result bridge (6), max_attempts removed (7)*