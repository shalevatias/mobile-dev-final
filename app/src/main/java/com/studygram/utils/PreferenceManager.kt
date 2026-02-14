package com.studygram.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME,
        Context.MODE_PRIVATE
    )

    var lastSyncTimestamp: Long
        get() = prefs.getLong(Constants.PREF_LAST_SYNC, 0)
        set(value) = prefs.edit().putLong(Constants.PREF_LAST_SYNC, value).apply()

    var userId: String?
        get() = prefs.getString(Constants.PREF_USER_ID, null)
        set(value) = prefs.edit().putString(Constants.PREF_USER_ID, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
