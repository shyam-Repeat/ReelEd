# SECTION 4: PARENT DISABLE CONTROL MECHANISM — R2

---

### 4.1 THE CORE PROBLEM

The parent and child share one phone and one Instagram or YouTube
account. When the parent picks up the phone to scroll for themselves,
the Foreground Service cannot distinguish parent usage from child
usage. If a quiz fires while the parent is scrolling, they will
resent the app and uninstall it. This is the single biggest trust
risk in the 10-tester rollout.

The solution requires two separate layers of control. The first is
the in-app PIN gate defined in the original Section 4 — this covers
settings, the dashboard, and permanent disable. The second is a
quick-access parent pause mechanism that works without opening the
app at all. Both layers require the same PIN to confirm identity,
but the quick-access path must be reachable in under three seconds
from anywhere.

---

### 4.2 TWO CONTROL LAYERS

LAYER 1 — QUICK PAUSE (new, defined in this section)
  Purpose  : Temporarily suspend quiz firing for a fixed duration
             while the parent is using the phone.
  Access   : From the persistent notification OR from the overlay
             card itself — without opening the app.
  PIN      : Required. Same 4-digit PIN set during onboarding.
  Effect   : Foreground Service stops triggering quizzes for the
             pause duration. Service stays alive. No quiz fires.
             When duration expires, quizzes resume automatically.
  Duration : Parent selects 30 minutes, 1 hour, or 2 hours.
             No indefinite pause in this layer — that is Layer 2.

LAYER 2 — FULL DISABLE (existing, unchanged from R1)
  Purpose  : Permanently stop the overlay until the parent
             re-enables it from inside the app.
  Access   : Parent dashboard inside the app, PIN required.
  Effect   : Foreground Service stops. Overlay permission is
             not revoked but the service will not start again
             until the parent manually re-enables from dashboard.

---

### 4.3 QUICK PAUSE — ACCESS POINT 1: PERSISTENT NOTIFICATION

The Foreground Service already shows a persistent notification
as required by Android. This notification is always visible in
the notification shade when the service is running. It is the
most accessible surface on the phone that does not require
opening the app.

The notification carries one action button labelled "Pause Quizzes".
Tapping this action button opens a minimal PIN entry screen as a
full-screen Activity with no navigation — not the main app.
This Activity exists solely for PIN verification in this context.

On correct PIN entry the parent selects a pause duration:
30 minutes, 1 hour, or 2 hours. The Activity closes immediately.
The notification updates its action button label to show the
remaining pause time, for example "Paused — 58 min remaining".
A second action button labelled "Resume Now" appears alongside it.
Tapping Resume Now requires no PIN — the parent already verified
identity to set the pause, so lifting it early requires no
additional friction.

On incorrect PIN entry the screen shakes and clears.
After three consecutive failures a 60-second lockout applies,
identical to the in-app PIN gate behaviour.

NOTIFICATION STATES

  Active state
    Text    : "Learning mode is on"
    Action  : "Pause Quizzes"

  Paused state
    Text    : "Paused — 58 min remaining"
    Actions : "Resume Now" and "Extend"
    Note    : Extend adds 30 minutes to the remaining time,
              no PIN required since identity is already verified
              for this pause session.

  Expired state (pause duration elapsed)
    Returns automatically to Active state.
    A brief notification appears: "Quiz mode resumed".

---

### 4.4 QUICK PAUSE — ACCESS POINT 2: OVERLAY CARD ITSELF

When a quiz card appears, the parent may be holding the phone.
They need a way to dismiss the quiz and pause further quizzes
without putting the phone down and opening the app.

Every quiz card — regardless of type or strict mode — shows a
small discreet element in the top-right corner of the card.
This element is a lock icon or a small label reading "Parent".
It is intentionally small and unlabelled to avoid the child
noticing it as an escape route.

Tapping this element opens the same minimal PIN entry Activity
described in Section 4.3. On correct PIN entry the parent sees
the same duration selector. The current quiz card is dismissed
as part of confirming the pause — the parent does not need to
also dismiss the card separately.

In strict mode the card covers the full screen with no dismiss
button for the child. The parent corner element must still be
present and tappable in strict mode. It is the only interactive
element on a strict card that is not part of the quiz interaction.

This element must be visually subtle enough that a child completing
a quiz does not consider it an exit route, but physically large
enough (minimum 44dp tap target) that a parent can tap it without
difficulty.

---

### 4.5 PAUSE STATE — BEHAVIOUR DEFINITION

When a pause is active the Foreground Service remains running.
The persistent notification remains visible.
The polling loop continues to run on its normal 30-second interval.
The trigger engine runs its gate checks as normal.

One additional gate is added at the top of the gate check sequence,
before all existing gates. If a pause is active and the pause
expiry time has not yet passed, the trigger engine returns Skip
immediately without evaluating any other gates. This gate is called
the Parent Pause Gate.

When the pause duration expires, the Parent Pause Gate clears
automatically on the next polling tick. No restart or user action
is required. The trigger engine resumes normal evaluation from
that tick onward including the full warmup and cooldown rules.
This means the first quiz after a pause will not fire immediately —
the warmup period applies as if a fresh session has started.

The pause state is stored in DataStore with two fields:
the boolean flag indicating a pause is active, and the epoch
millisecond timestamp at which the pause expires. Both are written
atomically when the parent confirms a pause duration.

---

### 4.6 CHILD CANNOT REACH QUICK PAUSE

The following design decisions prevent the child from using the
quick pause to escape quizzes.

  PIN is always required
    No pause takes effect without the 4-digit PIN. The child
    does not know the PIN. The PIN entry screen has no hint,
    no recovery option, and no bypass.

  The corner element is visually small and unlabelled
    Children completing a quiz focus on the answer options.
    The parent element is in the corner, small, and carries no
    text that suggests it is an exit. It uses a lock icon only.

  The notification action requires the notification shade
    Opening the notification shade and reading notification
    actions is not intuitive child behaviour during app usage.
    The Pause Quizzes button in the notification is an adult
    interaction pattern.

  Three failure lockout
    If a child discovers the corner element and attempts to guess
    the PIN, three wrong attempts lock the PIN screen for
    60 seconds. This is logged as an event in Room and synced
    to Supabase so the parent can see failed attempts on the
    dashboard.

---

### 4.7 IN-APP PIN GATE — UNCHANGED FROM R1

The following actions inside the app still require PIN verification
through the full in-app gate as defined in the original Section 4.

  Viewing the parent dashboard
  Changing trigger settings
  Permanently disabling the overlay (Layer 2 full disable)
  Re-enabling the overlay after a full disable

The quick pause mechanism in this section does not replace these.
It supplements them for the specific case of the parent picking up
the phone mid-session and needing immediate relief without context switching.

---

### 4.8 WHAT IS EXPLICITLY NOT BUILT IN MVP

  No biometric unlock
    Adds device-specific complexity. PIN is sufficient for 10 testers.

  No time-of-day schedule
    Defining "parent hours" vs "child hours" is a post-MVP feature.
    Quick pause covers the need for MVP.

  No per-app pause
    Pausing only for Instagram but not YouTube, or vice versa,
    is post-MVP. The pause applies to all target apps.

  No remote disable
    No web dashboard or SMS command to pause remotely.
    Parent is physically holding the phone in all MVP scenarios.

---

### 4.9 APP ENTRY ROUTING — UPDATED

The entry routing from R1 is unchanged in logic but the parent
quick pause path bypasses the main routing entirely. It opens
a standalone PIN Activity directly from the notification or
overlay tap. This Activity has no connection to the main app
navigation graph and closes itself after the pause is confirmed.
The child home screen, onboarding flow, and dashboard routing
are not affected.

PIN FLOW COMPOSABLE
kotlin@Composable
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

 PIN SETUP (ONBOARDING STEP)
kotlin@Composable
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
kotlin// DataStore keys
object PinKeys {
    val PARENT_PIN_HASH = stringPreferencesKey("parent_pin_hash")
    val PIN_SET = booleanPreferencesKey("pin_set")
    val FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
    val LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
}
APP ENTRY ROUTING LOGIC
kotlin// MainActivity.kt — on launch, route based on state

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
// For MVP: a small "Parent" text link at bottom of child_home screen → PIN →
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
---

*End of Section 4 — R2*
*PIN gate unchanged. Quick pause added via notification action*
*and overlay corner element. Child cannot reach either path*
*without the parent PIN.*