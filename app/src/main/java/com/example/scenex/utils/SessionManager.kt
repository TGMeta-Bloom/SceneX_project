package com.example.scenex.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Senior Engineer implementation for Global Session Management.
 * Fixed: Role checks are now case-insensitive to prevent navigation loops.
 */
object SessionManager {
    private const val PREF_NAME = "SceneX_Session"
    private const val KEY_ROLE = "user_role"
    private const val KEY_STATUS = "profile_status"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun establishSession(context: Context, userId: String, role: String?, status: String?) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ROLE, role?.uppercase()?.trim()) // Normalize on save
            putString(KEY_STATUS, status?.lowercase()?.trim()) // Normalize on save
            apply()
        }
    }

    fun setOnboardingSeen(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
    }

    fun hasSeenOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun getUserId(context: Context): String? = getPrefs(context).getString(KEY_USER_ID, null)
    fun getRole(context: Context): String? = getPrefs(context).getString(KEY_ROLE, null)
    fun getStatus(context: Context): String? = getPrefs(context).getString(KEY_STATUS, null)
    
    fun isTalent(context: Context) = getRole(context).equals("TALENT", ignoreCase = true)
    fun isRecruiter(context: Context) = getRole(context).equals("RECRUITER", ignoreCase = true)

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
