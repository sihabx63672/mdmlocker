package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.LockerActivity
import com.example.data.LockerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged

class LockerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: LockerRepository
    private var observationJob: Job? = null
    
    companion object {
        const val CHANNEL_ID = "locker_service_channel"
        const val NOTIFICATION_ID = 9999
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LockerService", "LockerService onCreate called")
        repository = LockerRepository(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Security protection active"))
        
        // Dynamic subscription to settings modifications
        startObservingSettings()
    }

    private fun startObservingSettings() {
        observationJob?.cancel()
        observationJob = serviceScope.launch {
            repository.settingsFlow
                .distinctUntilChanged { old, new -> old.isLocked == new.isLocked && old.wallpaperTheme == new.wallpaperTheme }
                .collect { settings ->
                    Log.d("LockerService", "Settings update collected: isLocked = ${settings.isLocked}")
                    if (settings.isLocked) {
                        launchLockerActivity()
                    } else {
                        // Notify receiver/activity to finish
                        val intent = Intent("com.example.ACTION_REMOTE_UNLOCK")
                        sendBroadcast(intent)
                    }
                }
        }
    }

    private fun launchLockerActivity() {
        Log.d("LockerService", "Launching LockerActivity...")
        val lockIntent = Intent(this, LockerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("FROM_SERVICE", true)
        }
        
        // Since Android 10, background activities are restricted, 
        // using full-screen intent is the high-security standard to bypass lock.
        startActivity(lockIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LockerService", "LockerService onStartCommand called")
        val forceLock = intent?.getBooleanExtra("TRIGGER_OVERLAY", false) ?: false
        if (forceLock) {
            serviceScope.launch {
                val settings = repository.getSettings()
                if (settings.isLocked) {
                    launchLockerActivity()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LockerService", "LockerService onDestroy called")
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Remote Security Core")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Remote Locker Security Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Monitors and enforces remote lock commands persistently."
                setSound(null, null)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
