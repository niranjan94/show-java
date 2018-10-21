package com.njlabs.showjava.utils

import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.njlabs.showjava.Constants

class Notifier(
    private val notificationManager: NotificationManager,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationId: Int = Constants.PROCESS_NOTIFICATION_ID,
    private val notificationTag: String?
) {

    var time: Long = 0

    public fun updateTitle(title: String) {
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setProgress(0, 0, true)
        notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
    }

    public fun updateText(text: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - time >= 500) {
            notificationBuilder.setContentText(text)
            notificationBuilder.setProgress(0, 0, true)
            notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
            time = currentTime
        }
    }

    public fun updateTitleText(title: String, text: String) {
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setContentText(text)
        notificationBuilder.setProgress(0, 0, true)
        notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
    }

    fun updateIntent(pendingIntent: PendingIntent) {
        notificationBuilder.setContentIntent(pendingIntent)
        notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
    }

    fun cancel() {
        notificationManager.cancel(notificationTag, notificationId)
    }

}