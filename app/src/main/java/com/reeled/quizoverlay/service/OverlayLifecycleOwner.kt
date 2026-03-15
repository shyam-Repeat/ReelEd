package com.reeled.quizoverlay.service

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

// ─────────────────────────────────────────────────────────────────────────────
// OverlayLifecycleOwner
//
// Responsibilities (layout doc):
//   Minimal LifecycleOwner + SavedStateRegistryOwner implementation required
//   to host a ComposeView inside a Service.
//
// Why this exists:
//   ComposeView requires a LifecycleOwner attached to the view tree via
//   ViewTreeLifecycleOwner. Activities and Fragments provide this automatically.
//   Services do not — this class bridges that gap for WindowManager-hosted views.
//
// Usage:
//   val lifecycleOwner = OverlayLifecycleOwner()
//   lifecycleOwner.onCreate()
//   composeView.setViewTreeLifecycleOwner(lifecycleOwner)
//   composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
//   // ... add view to WindowManager ...
//   lifecycleOwner.onStart()
//   lifecycleOwner.onResume()
//   // On removal:
//   lifecycleOwner.onPause()
//   lifecycleOwner.onStop()
//   lifecycleOwner.onDestroy()
//
// Dependency map (layout doc): service → (no additional internal deps here)
// ─────────────────────────────────────────────────────────────────────────────

class OverlayLifecycleOwner : SavedStateRegistryOwner {

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    // ── SavedState ────────────────────────────────────────────────────────────
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle event forwarding
    // Call these in sequence from OverlayForegroundService lifecycle callbacks.
    // ─────────────────────────────────────────────────────────────────────────

    fun onCreate() {
        // SavedState must be restored before the lifecycle moves past CREATED.
        // Passing null bundle — no state to restore for an overlay.
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SavedState — no-op save (overlay state is transient, not persisted)
    // ─────────────────────────────────────────────────────────────────────────

    fun performSave(outBundle: Bundle) {
        savedStateRegistryController.performSave(outBundle)
    }
}