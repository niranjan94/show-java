/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2018 Niranjan Rajendran
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.njlabs.showjava.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.receivers.DecompilerActionReceiver
import java.io.File

class ProcessNotifier(
    private val context: Context,
    private val notificationTag: String?,
    private val notificationId: Int = Constants.WORKER.PROGRESS_NOTIFICATION_ID
) {
    var time: Long = 0
    var isCancelled: Boolean = false

    private var manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var builder: NotificationCompat.Builder
    private lateinit var notification: Notification

    fun buildFor(title: String, packageName: String, packageLabel: String, packageFile: File ): ProcessNotifier {
        val stopIntent = Intent(context, DecompilerActionReceiver::class.java)
        stopIntent.action = Constants.WORKER.ACTION.STOP
        stopIntent.putExtra("id", packageName)
        stopIntent.putExtra("packageFilePath", packageFile.canonicalFile)
        stopIntent.putExtra("packageName", packageName)

        val pendingIntentForStop = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.WORKER.NOTIFICATION_CHANNEL,
                "Decompiler notification",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            manager.createNotificationChannel(channel)
        }

        val actionIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            R.drawable.ic_stop_black else R.drawable.ic_stat_stop

        builder = NotificationCompat.Builder(context, Constants.WORKER.NOTIFICATION_CHANNEL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentTitle(packageLabel)
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_stat_code)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .addAction(actionIcon, "Stop decompiler", pendingIntentForStop)
            .setOngoing(true)
            .setSound(null)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)

        manager.notify(
            notificationTag,
            Constants.WORKER.PROGRESS_NOTIFICATION_ID,
            silence(builder.build())
        )
        return this
    }

    private fun silence(notification: Notification): Notification {
        notification.sound = null
        notification.vibrate = null
        notification.defaults = notification.defaults and NotificationCompat.DEFAULT_SOUND.inv()
        notification.defaults = notification.defaults and NotificationCompat.DEFAULT_VIBRATE.inv()
        return notification
    }

    fun updateTitle(title: String) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && currentTime - time >= 500) {
            builder.setContentTitle(title)
            builder.setProgress(0, 0, true)
            manager.notify(notificationTag, notificationId, silence(builder.build()))
            time = currentTime
        }
    }

    fun updateText(text: String) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && currentTime - time >= 500) {
            builder.setContentText(text)
            builder.setProgress(0, 0, true)
            manager.notify(notificationTag, notificationId, silence(builder.build()))
            time = currentTime
        }
    }

    fun updateTitleText(title: String, text: String) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && currentTime - time >= 500) {
            builder.setContentTitle(title)
            builder.setContentText(text)
            builder.setProgress(0, 0, true)
            manager.notify(notificationTag, notificationId, silence(builder.build()))
            time = currentTime
        }
    }

    fun cancel() {
        isCancelled = true
        manager.cancel(notificationTag, notificationId)
    }
}