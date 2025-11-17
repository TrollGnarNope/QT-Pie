package com.veigar.questtracker.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class AppFirstInstall(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setFirstAppInstall() {
        sharedPreferences.edit { putBoolean(KEY_FIRST_INSTALL, false) }
    }

    fun isFirstAppInstall(): Boolean {
        Log.d("AppFirstInstall", "isFirstAppInstall: ${sharedPreferences.getBoolean(KEY_FIRST_INSTALL, true)}")
        return sharedPreferences.getBoolean(KEY_FIRST_INSTALL, true)
    }

    companion object {
        private const val PREFS_NAME = "com.veigar.questtracker.AppFirstInstallPrefs"
        private const val KEY_FIRST_INSTALL = "is_first_install"
    }
}