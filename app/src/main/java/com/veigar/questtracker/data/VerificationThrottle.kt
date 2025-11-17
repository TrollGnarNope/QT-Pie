package com.veigar.questtracker.data

import android.content.Context
import android.content.SharedPreferences

object VerificationThrottle {
    private const val PREFS = "verification_prefs"
    private const val KEY_PREFIX = "verification_resend_last_ts_"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun canResend(context: Context, uid: String?, nowMs: Long, minIntervalMs: Long = 60_000L): Boolean {
        if (uid.isNullOrBlank()) return false
        val last = prefs(context).getLong(KEY_PREFIX + uid, 0L)
        return (nowMs - last) >= minIntervalMs
    }

    fun recordResend(context: Context, uid: String?, nowMs: Long) {
        if (uid.isNullOrBlank()) return
        prefs(context).edit().putLong(KEY_PREFIX + uid, nowMs).apply()
    }

    fun getLastResendTime(context: Context, uid: String?): Long {
        if (uid.isNullOrBlank()) return 0L
        return prefs(context).getLong(KEY_PREFIX + uid, 0L)
    }
}



