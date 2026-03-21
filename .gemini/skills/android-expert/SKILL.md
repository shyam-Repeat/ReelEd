# Android Expert Skill

This skill combines expert guidance for building modern Android applications with Jetpack Compose and a systematic approach to verifying UI functionality using ADB commands on an emulator.

## Purpose
To provide a unified workflow for Android development, covering both production-quality UI implementation and autonomous E2E verification.

## When to Use
- Building new Android applications or features using Jetpack Compose.
- Implementing complex UI state management, navigation, and performance optimizations.
- Verifying UI changes through automated interaction and state verification.
- Debugging layout issues or interaction bugs on an Android Emulator.
- Capturing automated screenshots for documentation or PRs.

## 🛠 Prerequisites
- Android Emulator running.
- `adb` installed and in PATH.
- Android project set up with Jetpack Compose dependencies.

## 🚀 Workflow: Building with Jetpack Compose

### 1. State Management Pattern
Use `ViewModel` with `StateFlow` to expose UI state. Define a clear `UiState` data class.

```kotlin
data class UserUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)
```

### 2. Composition Patterns
Consume state in "Screen" composables and pass data down to stateless child components. Use `remember` and `derivedStateOf` to optimize recomposition.

### 3. Type-Safe Navigation
Define destinations as `@Serializable` objects or data classes and use `NavHost` with type-safe `composable<T>` calls.

## 🚀 Workflow: Mascot & Interaction

### 1. State-Driven Animations
Mascots should react to application state (e.g., correct/wrong answers). Use an `enum class` to define emotional states and `animate*AsState` or `InfiniteTransition` for fluid movement.

### 2. Composition with Canvas
For high-performance custom mascots, use `Canvas` to draw vector shapes. This ensures crisp scaling across all screen densities.

### 3. Rive Integration (Optional)
When using Rive, ensure the `.riv` asset is in `res/raw` and use the Rive Compose runtime for state-machine driven animations.

## 🚀 Workflow: UI Verification

### 1. Device Calibration & Discovery
Verify screen resolution and use `uiautomator dump` to find element bounds.
```bash
adb shell wm size
adb shell uiautomator dump /sdcard/view.xml && adb pull /sdcard/view.xml ./artifacts/view.xml
```

### 2. Interaction & Verification
- **Tap**: `adb shell input tap <x> <y>` (Use element center).
- **Screenshot**: `adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png ./artifacts/test_result.png`
- **Logcat**: `adb logcat -d | grep "TAG_NAME" | tail -n 20`

## 💡 Best Practices
- **Stability**: Mark UI state classes as `@Immutable` or `@Stable`.
- **Statelessness**: Keep child components stateless by passing lambdas for events.
- **Wait for Animations**: Add short sleeps (1-2s) between ADB interactions.
- **Fail Fast**: Stop if `uiautomator dump` doesn't find the expected element.
