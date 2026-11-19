package com.example.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R

/**
 * Keeps the Firestore real-time chat push notification engine alive when the user minimizes
 * DateMate or goes to the Android Home Screen ("App ke bahar"), so real Android OS System
 * Push Notifications pop up outside the app in real time.
 */
class DateMatePushMonitorService : Service() {

    override fun onCreate() {
        super.onCreate()
        PixoraNotificationManager.ensureChannel(applicationContext)
        DateMateBackgroundPushEngine.start(applicationContext)
        DateMateBackgroundPushEngine.refreshNow(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
            return START_NOT_STICKY
        }

        PixoraNotificationManager.ensureChannel(applicationContext)
        DateMateBackgroundPushEngine.start(applicationContext)
        DateMateBackgroundPushEngine.refreshNow(applicationContext)

        runCatching {
            val monitorNotification = NotificationCompat.Builder(
                applicationContext,
                PixoraNotificationManager.CHANNEL_BG_SYNC_ID
            )
                .setSmallIcon(R.drawable.ic_notification_pixora)
                .setContentTitle("DateMate Live Push Active")
                .setContentText("Listening for new messages & replies")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    MONITOR_NOTIFICATION_ID,
                    monitorNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(MONITOR_NOTIFICATION_ID, monitorNotification)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val MONITOR_NOTIFICATION_ID = 90421
        private const val ACTION_START = "com.example.notifications.START_PUSH_MONITOR"
        private const val ACTION_STOP = "com.example.notifications.STOP_PUSH_MONITOR"

        fun startMonitor(context: Context) {
            val appCtx = context.applicationContext
            runCatching {
                val intent = Intent(appCtx, DateMatePushMonitorService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(intent)
                } else {
                    appCtx.startService(intent)
                }
            }
        }

        fun stopMonitor(context: Context) {
            val appCtx = context.applicationContext
            runCatching {
                val intent = Intent(appCtx, DateMatePushMonitorService::class.java).apply {
                    action = ACTION_STOP
                }
                appCtx.stopService(intent)
            }
        }
    }
}
