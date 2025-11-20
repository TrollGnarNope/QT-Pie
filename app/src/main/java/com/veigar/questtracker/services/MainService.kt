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
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.flow.collectLatest

class MainService : Service(), NotificationDisplayer {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob) // Main for UI updates from service

    private lateinit var notificationMonitor: NotificationMonitor
    private var locationMonitor : LocationMonitor? = null
    private var geofenceMonitor : GeofenceMonitor? = null
    private var userProfileJob: Job? = null

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

        // FIX: Explicitly set foreground service type for Android 14+
        // Must match manifest: dataSync|location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ONGOING_NOTIFICATION_ID,
                createOngoingNotification("Initializing..."),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, createOngoingNotification("Initializing..."))
        }
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
        return START_STICKY
    }

    private fun start() {
        // Cancel any existing job to prevent duplicates
        userProfileJob?.cancel()

        userProfileJob = serviceScope.launch {
            // FIX: Observe user profile continuously.
            // This ensures if a child links a parent while the service is running,
            // monitoring starts IMMEDIATELY without restart.
            UserRepository.observeUserProfile().collectLatest { user ->
                if (user == null) {
                    Log.e(TAG, "User is null. Pausing monitoring.")
                    stopMonitors()
                    return@collectLatest
                }

                // Always start notification monitor for logged in users
                notificationMonitor.startMonitoring()

                if (user.role == "child") {
                    if (user.parentLinkedId.isNullOrBlank()) {
                        Log.d(TAG, "Child account, but no parent linked yet. Waiting...")
                        stopMonitors() // Stop if they unlinked
                    } else {
                        Log.d(TAG, "Parent linked: ${user.parentLinkedId}. Starting monitors.")

                        // Initialize monitors if null
                        if (locationMonitor == null) {
                            locationMonitor = LocationMonitor(applicationContext, user, serviceScope = serviceScope)
                        }
                        if (geofenceMonitor == null) {
                            geofenceMonitor = GeofenceMonitor(applicationContext, user, serviceScope)
                        }

                        // Start tracking
                        locationMonitor?.startTracking()
                        geofenceMonitor?.loadAndRegisterGeofences()
                        updateOngoingNotification("Monitoring active")
                    }
                } else {
                    // Parent role - typically doesn't need these monitors in background,
                    // but if you want parent location tracking, add here.
                    stopMonitors()
                }
            }
        }
    }

    private fun stopMonitors() {
        locationMonitor?.stopTracking()
        geofenceMonitor?.clearAllGeofences()
    }

    private fun stopServiceTasksAndSelf() {
        Log.d(TAG, "Stopping service tasks and service...")
        userProfileJob?.cancel()
        stopMonitors()
        notificationMonitor.stopMonitoring()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Network Callback Methods ---
    private fun initializeNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

                if (isInternet && isValidated) {
                    if (!isNetworkAvailable) {
                        isNetworkAvailable = true
                        Log.i(TAG, "Network available. Resuming monitoring.")
                        updateOngoingNotification("Network connected. Monitoring active.")
                        // Re-trigger start to ensure everything is running
                        start()
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if (isNetworkAvailable) {
                    // Check if there is truly no active network
                    if (connectivityManager.activeNetwork == null) {
                        isNetworkAvailable = false
                        Log.w(TAG, "Network lost.")
                        updateOngoingNotification("Network disconnected. Waiting...")
                        stopMonitors()
                    }
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
                connectivityManager.registerDefaultNetworkCallback(it)
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to register network callback", e)
            }
        }
    }

    fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
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
        userProfileJob?.cancel()
        unregisterNetworkCallback()
        stopMonitors()
        notificationMonitor.stopMonitoring()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(DAILY_TASKS_NOTIFICATION_ID)
        unregisterReceiver(taskUpdateReceiver)
        serviceJob.cancel()
        super.onDestroy()
    }

    // --- NotificationDisplayer Implementation ---

    override fun showNotification(notification: NotificationModel) {
        serviceScope.launch {
            synchronized(notificationQueue) {
                notificationQueue.removeAll { it.notificationId == notification.notificationId }
                notificationQueue.add(notification)
            }
        }
    }

    private fun startNotificationQueueProcessor() {
        notificationQueueJob = serviceScope.launch {
            while (isActive) {
                val notification = synchronized(notificationQueue) {
                    if (notificationQueue.isNotEmpty() && !isShowingNotification) {
                        isShowingNotification = true
                        val urgentIndex = notificationQueue.indexOfFirst { notif ->
                            notif.category == NotificationCategory.TASK_CHANGE ||
                                    notif.category == NotificationCategory.REWARD ||
                                    notif.category == NotificationCategory.SYSTEM
                        }
                        if (urgentIndex >= 0) notificationQueue.removeAt(urgentIndex) else notificationQueue.removeAt(0)
                    } else {
                        null
                    }
                }

                if (notification != null) {
                    displayNotification(notification)
                    delay(NOTIFICATION_QUEUE_DELAY_MS)
                    isShowingNotification = false
                } else {
                    delay(500)
                }
            }
        }
    }

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
        val uniqueId = (notification.notificationId.hashCode() xor System.currentTimeMillis().toInt()) and 0x7FFFFFFF

        notificationManager.notify(uniqueId, notificationBuilder.build())
    }

    override fun updateOngoingNotification(message: String) {
        serviceScope.launch {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(ONGOING_NOTIFICATION_ID, createOngoingNotification(message))
        }
    }

    private fun createDailyTasksNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tasksChannel = NotificationChannel(
                DAILY_TASKS_CHANNEL_ID,
                "Daily Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your daily tasks from QuestTracker."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(tasksChannel)
        }
    }

    private fun createOrUpdateDailyTasksNotification() {
        val taskInfo = dailyTaskNotifier.getTaskContentForNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tasksIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tasksPendingIntent = PendingIntent.getActivity(
            this,
            DAILY_TASKS_NOTIFICATION_ID,
            tasksIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, DAILY_TASKS_CHANNEL_ID)
            .setSmallIcon(R.drawable.to_do_list)
            .setContentTitle("DAILY TASKS")
            .setContentText(if (taskInfo.hasTasks) taskInfo.contentText else "No tasks for today!")
            .setContentIntent(tasksPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (taskInfo.hasTasks) {
            builder.setStyle(taskInfo.style)
        }

        notificationManager.notify(DAILY_TASKS_NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "QuestTracker Updates",
                NotificationManager.IMPORTANCE_DEFAULT
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
                "QuestTracker Important",
                NotificationManager.IMPORTANCE_HIGH
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
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
    }
}