package com.njlabs.showjava.utils

import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat

class Notifier(
    private val notificationManager: NotificationManager,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationId: Int = 0
) {

    var time: Long = 0

    fun updateTitle(title: String) {
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setProgress(0, 0, true)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun updateText(text: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - time >= 500) {
            notificationBuilder.setContentText(text)
            notificationBuilder.setProgress(0, 0, true)
            notificationManager.notify(notificationId, notificationBuilder.build())
            time = currentTime
        }
    }

    fun updateTitleText(title: String, text: String) {
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setContentText(text)
        notificationBuilder.setProgress(0, 0, true)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun updateIntent(pendingIntent: PendingIntent) {
        notificationBuilder.setContentIntent(pendingIntent)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun cancel() {
        notificationManager.cancel(notificationId)
    }

}