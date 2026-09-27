package com.vidhya.focuslock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * The enforcement engine. Every time the foreground window changes, we check:
 *  1. Is a focus session currently active?
 *  2. Is the package that just came to the front allowed?
 * If a session is active and the app isn't allowed, we bounce the user home
 * and show the block screen. This also covers the most common bypass attempts:
 * opening Settings > Accessibility (to turn this service off) or Settings > Apps
 * (to force-stop / uninstall) are themselves blocked like any other disallowed app.
 */
class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val SETTINGS_PKG = "com.android.settings"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (!SessionManager.isSessionActive(applicationContext)) return
        if (SessionManager.isAllowed(applicationContext, pkg)) return

        // Block Settings entirely during a session — this is what stops the
        // user from disabling the accessibility service, revoking device
        // admin, or uninstalling the app mid-session.
        if (pkg == SETTINGS_PKG || pkg == packageName) {
            if (pkg == packageName) return // our own block screen, allow it
        }

        // Not allowed: go home, then show the block screen on top.
        performGlobalAction(GLOBAL_ACTION_HOME)
        val intent = Intent(this, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockActivity.EXTRA_BLOCKED_PKG, pkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() { /* no-op */ }
}
