package com.veigar.questtracker.services.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.veigar.questtracker.R
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationData
import com.veigar.questtracker.model.NotificationModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.veigar.questtracker.ACTION_GEOFENCE") {
            Log.e("GeofenceReceiver", "Received unknown or invalid intent")
            return
        }
        val event = GeofencingEvent.fromIntent(intent)
        if (event?.hasError() ?: true) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(event?.errorCode ?: 0)
            Log.e("GeofenceReceiver", "Error: ${errorMessage}")
            return
        }
        Log.i("GeofenceReceiver", "Received valid geofence transition")

        val triggeringGeofences = event.triggeringGeofences
        val transitionType = event.geofenceTransition

        Log.d("GeofenceReceiver", "Geofence transition type: $transitionType")
        Log.d("GeofenceReceiver", "Number of triggering geofences: ${triggeringGeofences?.size ?: 0}")

        val transition = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> {
                Log.w("GeofenceReceiver", "Unknown transition type: $transitionType")
                "UNKNOWN"
            }
        }
        
        Log.d("GeofenceReceiver", "Processed transition: $transition")

        if (triggeringGeofences != null) {
            for (geofence in triggeringGeofences) {
                val geofenceId = geofence.requestId
                val parts = geofenceId.split("|")
                val placeName = parts.getOrNull(0) ?: "Unknown Place"
                val parentId = parts.getOrNull(1) ?: ""
                val childName = parts.getOrNull(2) ?: "Your child"

                val timestamp = System.currentTimeMillis()
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )

                val (title, message) = when (transition) {
                    "ENTER" -> "$childName has arrived at $placeName" to
                            "$childName just entered $placeName"

                    "EXIT" -> "$childName has left $placeName" to
                            "$childName just exited $placeName. Keep an eye out!"

                    else -> "Unknown transition" to
                            "An unknown geofence event occurred."
                }

                // 🔔 Send cloud notification to the parent
                GlobalScope.launch(Dispatchers.IO) {
                    NotificationsRepository.sendNotification(
                        targetId = parentId,
                        notification = NotificationModel(
                            title = title,
                            message = message,
                            timestamp = timestamp,
                            category = NotificationCategory.LOCATION_UPDATE,
                            notificationData = NotificationData(
                                content = "",
                                action = "parent"
                            )
                        )
                    )
                }

                // 🔕 Show local notification
                //for debugging purposes only
//                showOfflineNotification(
//                    context,
//                    title,
//                    "$message\n$relativeTime"
//                )

                Log.i("GeofenceReceiver", "Transition: $transition for Geofence ID: $geofenceId")
            }
        }
    }

    private fun showOfflineNotification(context: Context, title: String, message: String) {
        val channelId = "geofence_alerts"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggered geofences while offline"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.heart)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}