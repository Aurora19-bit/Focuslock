package com.vidhya.focuslock

import android.content.Context

/**
 * Single source of truth for whether a focus session is active, when it ends,
 * and which package names are allowed during a session.
 * Backed by SharedPreferences so it survives process death and reboot.
 */
object SessionManager {
    private const val PREFS = "focuslock_prefs"
    private const val KEY_END_TIME = "session_end_time"
    private const val KEY_ALLOWED = "allowed_packages"

    // Packages that must ALWAYS be reachable, even mid-session, so the phone
    // stays usable (dialer, our own app, the default launcher).
    fun corePackages(context: Context): Set<String> {
        val launcher = context.packageManager.resolveActivity(
            android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME),
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName

        return setOfNotNull(
            context.packageName,
            launcher,
            "com.android.dialer",
            "com.android.phone",
            "com.android.server.telecom"
        )
    }

    fun isSessionActive(context: Context): Boolean {
        val end = prefs(context).getLong(KEY_END_TIME, 0L)
        return end > System.currentTimeMillis()
    }

    fun sessionEndTime(context: Context): Long = prefs(context).getLong(KEY_END_TIME, 0L)

    fun startSession(context: Context, durationMillis: Long) {
        prefs(context).edit()
            .putLong(KEY_END_TIME, System.currentTimeMillis() + durationMillis)
            .apply()
    }

    /** Ends the session immediately. Only ever called from inside the app itself
     * (e.g. an explicit "end early" flow you design), never from a blocked app. */
    fun endSessionNow(context: Context) {
        prefs(context).edit().putLong(KEY_END_TIME, 0L).apply()
    }

    fun getAllowedPackages(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_ALLOWED, emptySet())?.toSet() ?: emptySet()
    }

    fun setAllowedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_ALLOWED, packages).apply()
    }

    fun isAllowed(context: Context, packageName: String): Boolean {
        return packageName in corePackages(context) || packageName in getAllowedPackages(context)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
