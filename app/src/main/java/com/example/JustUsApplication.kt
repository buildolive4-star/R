package com.example

import android.app.Application
import com.example.notifications.DateMateBackgroundPushEngine
import com.example.notifications.OneSignalPushManager
import com.example.notifications.PixoraNotificationManager

class JustUsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PixoraNotificationManager.ensureChannel(this)
        OneSignalPushManager.init(this)
        DateMateBackgroundPushEngine.start(this)
    }
}
