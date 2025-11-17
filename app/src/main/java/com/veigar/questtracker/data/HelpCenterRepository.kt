package com.veigar.questtracker.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.veigar.questtracker.model.HelpCenterRequest
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object HelpCenterRepository {
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()
    private const val COLLECTION_NAME = "helpcenter"
    private const val PREFS_NAME = "help_center_prefs"
    private const val CRASH_LOG_KEY = "last_crash_log"
    private const val MAX_CRASH_LOGS = 3

    /**
     * Submit a help center request to Firebase Firestore
     */
    suspend fun submitHelpRequest(request: HelpCenterRequest): Result<Unit> = try {
        val documentId = "${request.userId}_${request.timestamp}"
        firestore.collection(COLLECTION_NAME)
            .document(documentId)
            .set(request)
            .await()
        
        Log.d("HelpCenterRepo", "Successfully submitted help request: ${request.requestId}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("HelpCenterRepo", "Error submitting help request", e)
        Result.failure(e)
    }

    /**
     * Save crash log to SharedPreferences
     */
    fun saveCrashLog(context: Context, crashLog: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Get existing crash logs
        val existingLogs = getCrashLogs(context).toMutableList()
        
        // Add new crash log at the beginning
        existingLogs.add(0, crashLog)
        
        // Keep only the last MAX_CRASH_LOGS
        val trimmedLogs = existingLogs.take(MAX_CRASH_LOGS)
        
        // Save as JSON array
        val logsJson = trimmedLogs.joinToString(separator = "|||CRASH_LOG_SEPARATOR|||")
        editor.putString(CRASH_LOG_KEY, logsJson)
        editor.apply()
        
        Log.d("HelpCenterRepo", "Saved crash log, total logs: ${trimmedLogs.size}")
    }

    /**
     * Get all crash logs from SharedPreferences
     */
    fun getCrashLogs(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logsJson = prefs.getString(CRASH_LOG_KEY, "") ?: ""
        
        return if (logsJson.isNotEmpty()) {
            logsJson.split("|||CRASH_LOG_SEPARATOR|||").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }

    /**
     * Get the most recent crash log
     */
    fun getLastCrashLog(context: Context): String? {
        val logs = getCrashLogs(context)
        return logs.firstOrNull()
    }

    /**
     * Clear all crash logs after successful submission
     */
    fun clearCrashLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(CRASH_LOG_KEY)
        editor.apply()
        
        Log.d("HelpCenterRepo", "Cleared all crash logs")
    }

    /**
     * Get device information for bug reports
     */
    fun getDeviceInfo(): String {
        return buildString {
            appendLine("Device: ${android.os.Build.MODEL}")
            appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
            appendLine("API Level: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        }
    }
}



