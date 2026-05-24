package com.example.scenex.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Senior Engineer implementation for Global Session Management.
 * Handles role-based permissions and profile state persistence.
 */
object SessionManager {
    private const val PREF_NAME = "SceneX_Session"
    private const val KEY_ROLE = "user_role"
    private const val KEY_STATUS = "profile_status"
    private const val KEY_USER_ID = "user_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun establishSession(context: Context, userId: String, role: String?, status: String?) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ROLE, role)
            putString(KEY_STATUS, status)
            apply()
        }
    }

    fun getRole(context: Context): String? = getPrefs(context).getString(KEY_ROLE, null)
    fun getStatus(context: Context): String? = getPrefs(context).getString(KEY_STATUS, null)
    
    fun isTalent(context: Context) = getRole(context) == "TALENT"
    fun isRecruiter(context: Context) = getRole(context) == "RECRUITER"
    fun isVerified(context: Context) = getStatus(context) == "verified"

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
