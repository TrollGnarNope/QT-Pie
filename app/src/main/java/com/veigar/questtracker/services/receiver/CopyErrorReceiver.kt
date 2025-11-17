package com.veigar.questtracker.services.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class CopyErrorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val errorDetails = intent?.getStringExtra("error_details")
        if (context != null && errorDetails != null) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Error Details", errorDetails)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                context,
                "Error copied to clipboard",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}