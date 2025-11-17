package com.veigar.questtracker.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.veigar.questtracker.MainActivity
import com.veigar.questtracker.R
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationModel
import kotlinx.coroutines.*

class MainService : Service(), NotificationDisplayer {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob) // Main for UI updates from service

    private lateinit var notificationMonitor: NotificationMonitor
    private var locationMonitor : LocationMonitor? = null
    private var geofenceMonitor : GeofenceMonitor? = null

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isNetworkAvailable = false

    private lateinit var dailyTaskNotifier: DailyTaskNotifier

    companion object {
        private const val TAG = "MainService"
        const val ACTION_START_MONITORING = "com.veigar.questtracker.services.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.veigar.questtracker.services.action.STOP_MONITORING"

        const val ACTION_TASK_DATA_UPDATED_FOR_SERVICE = "com.veigar.questtracker.services.action.TASK_DATA_UPDATED"
        private const val NOTIFICATION_CHANNEL_ID = "MainServiceChannel"
        private const val URGENT_NOTIFICATION_CHANNEL_ID = "UrgentNotificationsChannel"

        private const val DAILY_TASKS_CHANNEL_ID = "DailyTasksChannel"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val DAILY_TASKS_NOTIFICATION_ID = 2
        private const val NOTIFICATION_QUEUE_DELAY_MS = 2000L // 2 seconds between notifications
    }
    
    // Notification queue to prevent stacking
    private val notificationQueue = mutableListOf<NotificationModel>()
    private var isShowingNotification = false
    private var notificationQueueJob: Job? = null

    // BroadcastReceiver to listen for task data updates
    private val taskUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TASK_DATA_UPDATED_FOR_SERVICE) {
                Log.d(TAG, "Received task data update broadcast. Refreshing notification.")
                createOrUpdateDailyTasksNotification()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        dailyTaskNotifier = DailyTaskNotifier(applicationContext)
        createNotificationChannel()
        createUrgentNotificationChannel()
        createDailyTasksNotificationChannel()
        notificationMonitor = NotificationMonitor(applicationContext, this)
        startNotificationQueueProcessor()

        // --- Network Connectivity Initialization ---
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        initializeNetworkCallback()
        registerNetworkCallback()

        val intentFilter = IntentFilter(ACTION_TASK_DATA_UPDATED_FOR_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(taskUpdateReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(taskUpdateReceiver, intentFilter)
        }
        startForeground(ONGOING_NOTIFICATION_ID, createOngoingNotification("Initializing..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                start() // Call start when monitoring begins
            }
            ACTION_STOP_MONITORING -> {
                stopServiceTasksAndSelf()
            }
        }
        return START_STICKY // Or START_NOT_STICKY if you don't want it to auto-restart
    }

    private fun start() {
        serviceScope.launch {
            val user = UserRepository.getUserProfile()
            if (user == null) {
                Log.e(TAG, "User is null. Cannot start monitoring.")
                return@launch
            }
            notificationMonitor.startMonitoring()
            if(user.role == "child"){
                if(user.parentLinkedId.isNullOrBlank()){
                    Log.e(TAG, "Parent not linked. Cannot start monitoring.")
                    return@launch
                }
                Log.d(TAG, "Starting location monitoring for ${user.getDecodedUid()}")
                locationMonitor = LocationMonitor(applicationContext, user, serviceScope = serviceScope)
                locationMonitor?.startTracking()
                geofenceMonitor = GeofenceMonitor(applicationContext, user, serviceScope)
                geofenceMonitor?.loadAndRegisterGeofences()
            }
        }
    }
    private fun stopServiceTasksAndSelf() {
        Log.d(TAG, "Stopping service tasks and service...")
        notificationMonitor.stopMonitoring()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Network Callback Methods ---
    private fun initializeNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // When a network becomes available, check its capabilities immediately
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

                if (isInternet && isValidated) {
                    if (!isNetworkAvailable) { // Only update if state actually changed
                        isNetworkAvailable = true
                        Log.i(TAG, "Network available and validated. Connected to: $network")
                        updateOngoingNotification("Network connected. Monitoring active.")
                        locationMonitor?.startTracking()
                        notificationMonitor.startMonitoring()
                        // If there are tasks that were pending due to no network, you might trigger them here.
                        // e.g., serviceScope.launch { attemptPendingUploads() }
                    } else {
                        Log.i(TAG, "Network available and validated, but already marked as available. Network: $network")
                    }
                } else {
                    // Network available but not yet validated or no internet. Keep current state or indicate partial.
                    Log.w(TAG, "Network available ($network) but not yet validated or no internet. Has Internet? $isInternet, Is Validated? $isValidated")
                    // Do not set isNetworkAvailable to true here, wait for onCapabilitiesChanged for validation.
                    if (isNetworkAvailable) { // If it was previously available but now the default has issues
                        // This case might be better handled by onCapabilitiesChanged for the default network
                        // or if the *active* network loses capabilities.
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // This is called when the *default* network (monitored by registerDefaultNetworkCallback) is lost.
                // Check if there's any active network left that can provide internet.
                val activeNetwork = connectivityManager.activeNetwork // This is the *current* default network after the loss
                if (activeNetwork == null) {
                    // No active network means no internet connection via the default path
                    if (isNetworkAvailable) { // Only update if state actually changed
                        isNetworkAvailable = false
                        Log.w(TAG, "Network lost. No active network connection. Lost Network: $network")
                        updateOngoingNotification("Network disconnected. Waiting...")
                        locationMonitor?.stopTracking()
                        notificationMonitor.stopMonitoring()
                    } else {
                        Log.i(TAG, "Network lost, but already marked as unavailable. Lost Network: $network")
                    }
                } else {
                    // A network was lost, but another one (activeNetwork) is still available and likely the new default.
                    Log.i(TAG, "Default network ($network) was lost, but another network is still active: $activeNetwork")
                    // Re-evaluate the capabilities of the *new* active network
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    val isInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

                    if (isInternet && isValidated) {
                        // New active network is good, state remains available
                        Log.i(TAG, "New active network ($activeNetwork) is validated and has internet.")
                        // isNetworkAvailable should already be true if the system seamlessly switched.
                    } else {
                        // New active network does not have internet or is not validated.
                        if (isNetworkAvailable) { // Only update if state actually changed
                            isNetworkAvailable = false
                            Log.w(TAG, "New active network ($activeNetwork) does not have internet or is not validated.")
                            updateOngoingNotification("Network connectivity issue. Waiting...")
                            locationMonitor?.stopTracking()
                            notificationMonitor.stopMonitoring()
                        }
                    }
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                Log.i(TAG, "Network capabilities changed for $network: Has Internet? $isInternet, Is Validated? $isValidated")

                // We only care about the capabilities of the *currently active* default network
                val activeNetwork = connectivityManager.activeNetwork

                if (activeNetwork == network) { // Check if the changed network is our current default
                    if (isInternet && isValidated) {
                        if (!isNetworkAvailable) { // If it was previously marked as unavailable
                            isNetworkAvailable = true
                            Log.i(TAG, "Active network ($network) re-validated and has internet. Monitoring active.")
                            updateOngoingNotification("Network connected. Monitoring active.")
                            locationMonitor?.startTracking()
                            notificationMonitor.startMonitoring()
                        }
                    } else {
                        // If the current default network loses internet or validation
                        if (isNetworkAvailable) { // Only update if state actually changed
                            isNetworkAvailable = false
                            Log.w(TAG, "Active network ($network) no longer has internet or is not validated.")
                            updateOngoingNotification("Network connectivity issue. Waiting...")
                            locationMonitor?.stopTracking()
                            notificationMonitor.stopMonitoring()
                        }
                    }
                } else {
                    Log.i(TAG, "Capabilities changed for a non-default network: $network. Current default: $activeNetwork")
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                // Called when the framework has indicated it will not be able to satisfy this request (default network).
                // This is a strong indication of no internet connectivity.
                if (isNetworkAvailable) { // Only update if state actually changed
                    isNetworkAvailable = false
                    Log.w(TAG, "Default network unavailable for the request. No active internet.")
                    updateOngoingNotification("Network unavailable. Waiting...")
                    locationMonitor?.stopTracking()
                    notificationMonitor.stopMonitoring()
                } else {
                    Log.i(TAG, "Default network unavailable, but already marked as unavailable.")
                }
            }
        }
    }

    fun registerNetworkCallback() {
        if (networkCallback == null) {
            initializeNetworkCallback()
        }
        networkCallback?.let {
            try {
                // Register for the default network callback to get overall internet connectivity status
                connectivityManager.registerDefaultNetworkCallback(it)
                Log.i(TAG, "Default Network callback registered.")

                // Check initial state
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    if (capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        isNetworkAvailable = true
                        Log.i(TAG, "Initial network check: Network available and validated.")
                        locationMonitor?.startTracking()
                        notificationMonitor.startMonitoring()
                        updateOngoingNotification("Monitoring active...")
                    } else {
                        isNetworkAvailable = false
                        Log.w(TAG, "Initial network check: Network available but no internet/not validated.")
                        locationMonitor?.stopTracking()
                        notificationMonitor.stopMonitoring()
                        updateOngoingNotification("Network connectivity issue. Waiting...")
                    }
                } else {
                    isNetworkAvailable = false
                    Log.w(TAG, "Initial network check: No active network.")
                    locationMonitor?.stopTracking()
                    notificationMonitor.stopMonitoring()
                    updateOngoingNotification("Waiting for network...")
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to register network callback due to SecurityException. Check permissions.", e)
                // Handle the case where permission might be missing or revoked at runtime
                updateOngoingNotification("Permission error: Network monitoring unavailable.")
                locationMonitor?.stopTracking()
                notificationMonitor.stopMonitoring()
            }
        }
    }

    fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
                Log.i(TAG, "Network callback unregistered.")
            } catch (e: Exception) { // Can sometimes throw IllegalArgumentException if not registered
                Log.w(TAG, "Error unregistering network callback: ${e.message}")
            }
            networkCallback = null
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // Not a bound service
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        notificationQueueJob?.cancel()
        unregisterNetworkCallback()
        notificationMonitor.stopMonitoring()
        locationMonitor?.stopTracking()
        geofenceMonitor?.clearAllGeofences()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(DAILY_TASKS_NOTIFICATION_ID)
        unregisterReceiver(taskUpdateReceiver)
        serviceJob.cancel()
        super.onDestroy()
    }

    // --- NotificationDisplayer Implementation ---

    override fun showNotification(notification: NotificationModel) {
        // Add to queue instead of showing immediately
        serviceScope.launch {
            synchronized(notificationQueue) {
                // Remove duplicate notifications with same ID
                notificationQueue.removeAll { it.notificationId == notification.notificationId }
                notificationQueue.add(notification)
                Log.d(TAG, "Added notification to queue: ${notification.title}. Queue size: ${notificationQueue.size}")
            }
        }
    }
    
    /**
     * Processes the notification queue one at a time with delays to prevent stacking
     * Urgent notifications are prioritized and shown first
     */
    private fun startNotificationQueueProcessor() {
        notificationQueueJob = serviceScope.launch {
            while (isActive) {
                val notification = synchronized(notificationQueue) {
                    if (notificationQueue.isNotEmpty() && !isShowingNotification) {
                        isShowingNotification = true
                        // Prioritize urgent notifications (task changes, rewards, system)
                        val urgentIndex = notificationQueue.indexOfFirst { notif ->
                            notif.category == NotificationCategory.TASK_CHANGE ||
                            notif.category == NotificationCategory.REWARD ||
                            notif.category == NotificationCategory.SYSTEM
                        }
                        if (urgentIndex >= 0) {
                            notificationQueue.removeAt(urgentIndex)
                        } else {
                            notificationQueue.removeAt(0)
                        }
                    } else {
                        null
                    }
                }
                
                if (notification != null) {
                    displayNotification(notification)
                    // Shorter delay for urgent notifications to feel more immediate
                    val isUrgent = notification.category == NotificationCategory.TASK_CHANGE || 
                                  notification.category == NotificationCategory.REWARD ||
                                  notification.category == NotificationCategory.SYSTEM
                    delay(if (isUrgent) NOTIFICATION_QUEUE_DELAY_MS / 2 else NOTIFICATION_QUEUE_DELAY_MS)
                    isShowingNotification = false
                } else {
                    delay(500) // Check queue every 500ms
                }
            }
        }
    }
    
    /**
     * Actually displays the notification with appropriate priority and urgency
     */
    private suspend fun displayNotification(notification: NotificationModel) {
        NotificationsRepository.setNotificationRead(UserRepository.currentUserId()!!, notification.notificationId)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("notification_id_extra", notification.notificationId)
            putExtra("notification_type_extra", notification.category.name)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notification.notificationId.hashCode(),
            intent,
            pendingIntentFlags
        )

        // Determine priority and channel based on category
        val isUrgent = notification.category == NotificationCategory.TASK_CHANGE || 
                      notification.category == NotificationCategory.REWARD ||
                      notification.category == NotificationCategory.SYSTEM
        
        val channelId = if (isUrgent) URGENT_NOTIFICATION_CHANNEL_ID else NOTIFICATION_CHANNEL_ID
        val priority = if (isUrgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.heart)
            .setContentTitle(notification.title ?: "New Notification")
            .setContentText(notification.message ?: "You have a new update.")
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(if (isUrgent) NotificationCompat.DEFAULT_ALL else NotificationCompat.DEFAULT_LIGHTS)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message ?: "You have a new update."))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use a combination of notificationId hash and timestamp for unique IDs
        // This ensures each notification gets a unique system ID even if they arrive simultaneously
        val uniqueId = (notification.notificationId.hashCode() xor System.currentTimeMillis().toInt()) and 0x7FFFFFFF
        
        notificationManager.notify(uniqueId, notificationBuilder.build())
        Log.d(TAG, "Displayed system notification: ${notification.title} (Urgent: $isUrgent, ID: $uniqueId)")
    }

    override fun updateOngoingNotification(message: String) {
        // Use serviceScope (Main dispatcher)
        serviceScope.launch {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(ONGOING_NOTIFICATION_ID, createOngoingNotification(message))
        }
    }

    private fun createDailyTasksNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tasksChannel = NotificationChannel(
                DAILY_TASKS_CHANNEL_ID,
                "Daily Tasks", // User-visible name
                NotificationManager.IMPORTANCE_LOW // Or .DEFAULT if you want it more prominent but still ongoing
            ).apply {
                description = "Shows your daily tasks from QuestTracker."
                setShowBadge(false) // Typically for ongoing notifications
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(tasksChannel)
        }
    }

    private fun createOrUpdateDailyTasksNotification() {
        val taskInfo = dailyTaskNotifier.getTaskContentForNotification() // Or getTaskContentAsBigText()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent for when the tasks notification is tapped
        val tasksIntent = Intent(this, MainActivity::class.java).apply {
            // Optionally, add extras to navigate to a specific tasks screen
            // putExtra("navigate_to", NavRoutes.TasksScreen.route)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tasksPendingIntent = PendingIntent.getActivity(
            this,
            DAILY_TASKS_NOTIFICATION_ID, // Use the notification ID as request code for PendingIntent
            tasksIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (taskInfo.hasTasks) {
            val builder = NotificationCompat.Builder(this, DAILY_TASKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.to_do_list) // A specific icon for tasks is good
                .setContentTitle("DAILY TASKS")
                .setContentText(taskInfo.contentText)
                .setStyle(taskInfo.style)
                .setContentIntent(tasksPendingIntent)
                .setOngoing(true) // Makes it persistent like the primary one
                .setOnlyAlertOnce(true) // No sound/vibrate on updates
                .setPriority(NotificationCompat.PRIORITY_LOW) // Or .DEFAULT

            notificationManager.notify(DAILY_TASKS_NOTIFICATION_ID, builder.build())
            Log.d(TAG, "Daily Tasks notification updated/shown with ${taskInfo.contentText}")
        } else {
            val builder = NotificationCompat.Builder(this, DAILY_TASKS_CHANNEL_ID)
                .setSmallIcon(R.drawable.to_do_list)
                .setContentTitle("DAILY TASKS")
                .setContentText("No tasks for today!")
                .setContentIntent(tasksPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
            notificationManager.notify(DAILY_TASKS_NOTIFICATION_ID, builder.build())
            Log.d(TAG, "Daily Tasks notification shown: No tasks.")
        }
    }

    // --- Helper methods for Foreground Service Notification ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "QuestTracker Updates", // User-visible name
                NotificationManager.IMPORTANCE_DEFAULT // Default importance for regular notifications
            ).apply {
                description = "General app notifications and updates."
                enableVibration(false)
                enableLights(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
    
    private fun createUrgentNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val urgentChannel = NotificationChannel(
                URGENT_NOTIFICATION_CHANNEL_ID,
                "QuestTracker Important", // User-visible name
                NotificationManager.IMPORTANCE_HIGH // High importance for urgent notifications
            ).apply {
                description = "Urgent notifications like task approvals, rewards, and important updates."
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(urgentChannel)
        }
    }

    private fun createOngoingNotification(text: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("QuestTracker")
            .setContentText(text)
            .setSmallIcon(R.drawable.heart)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes it a non-dismissable foreground notification
            .setPriority(NotificationCompat.PRIORITY_LOW) // For less intrusive ongoing notifications
            .setOnlyAlertOnce(true) // Don't make sound/vibrate for subsequent updates
            .build()
    }
}
