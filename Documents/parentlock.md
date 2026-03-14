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