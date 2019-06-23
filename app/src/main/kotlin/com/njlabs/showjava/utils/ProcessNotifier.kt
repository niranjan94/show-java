/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2019 Niranjan Rajendran
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
import com.njlabs.showjava.activities.ContainerActivity
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.fragments.decompiler.DecompilerFragment
import com.njlabs.showjava.fragments.decompiler.DecompilerProcessFragment
import com.njlabs.showjava.fragments.decompiler.LowMemoryFragment
import com.njlabs.showjava.fragments.explorer.navigator.NavigatorFragment
import com.njlabs.showjava.receivers.DecompilerActionReceiver
import com.njlabs.showjava.utils.ktx.sourceDir
import java.io.File


/**
 * Takes care of creating, updates progress notifications along with success and failure notifications
 * for the decompiler process.
 */
class ProcessNotifier(
    private val context: Context,
    private val notificationTag: String?,
    private val notificationId: Int = Constants.WORKER.PROGRESS_NOTIFICATION_ID
) {
    private var time: Long = 0
    private var isCancelled: Boolean = false

    private var manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var builder: NotificationCompat.Builder
    private lateinit var notification: Notification
    private lateinit var packageName: String
    private lateinit var packageLabel: String
    private lateinit var packageFile: File

    fun withPackageInfo(packageName: String, packageLabel: String, packageFile: File): ProcessNotifier {
        this.packageName = packageName
        this.packageFile = packageFile
        this.packageLabel = packageLabel
        return this
    }

    fun buildFor(title: String, packageName: String, packageLabel: String, packageFile: File, decompilerIndex: Int): ProcessNotifier {

        this.packageName = packageName
        this.packageFile = packageFile
        this.packageLabel = packageLabel

        val stopIntent = Intent(context, DecompilerActionReceiver::class.java)
        stopIntent.action = Constants.WORKER.ACTION.STOP
        stopIntent.putExtra("id", packageName)
        stopIntent.putExtra("packageFilePath", packageFile.canonicalFile)
        stopIntent.putExtra("packageName", packageName)
        val pendingIntentForStop = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val viewIntent = Intent(context, ContainerActivity::class.java)
        viewIntent.putExtra("packageInfo", PackageInfo(packageLabel, packageName))
        viewIntent.putExtra("decompilerIndex", decompilerIndex)
        viewIntent.putExtra("fragmentClass", DecompilerProcessFragment::class.java.name)

        val manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntentForView = PendingIntent.getActivity(
            context,
            0,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.WORKER.PROGRESS_NOTIFICATION_CHANNEL,
                "Decompiler notification",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            manager.createNotificationChannel(channel)
        }

        val actionIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            R.drawable.ic_stop_black else R.drawable.ic_stat_stop

        builder = NotificationCompat.Builder(context, Constants.WORKER.PROGRESS_NOTIFICATION_CHANNEL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentTitle(packageLabel)
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_stat_code)
            .setContentIntent(pendingIntentForView)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .addAction(actionIcon, "Stop decompiler", pendingIntentForStop)
            .setOngoing(true)
            .setSound(null)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)

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

    fun updateTitle(title: String, forceSet: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && (currentTime - time >= 500 || forceSet) && ::builder.isInitialized) {
            builder.setContentTitle(title)
            manager.notify(notificationTag, notificationId, silence(builder.build()))
            time = currentTime
        }
    }

    fun updateText(text: String, forceSet: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && (currentTime - time >= 500 || forceSet) && ::builder.isInitialized) {
            builder.setContentText(text)
            manager.notify(notificationTag, notificationId, silence(builder.build()))
            time = currentTime
        }
    }

    fun updateTitleText(title: String, text: String, forceSet: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && (currentTime - time >= 500 || forceSet) && ::builder.isInitialized) {
            builder.setContentTitle(title)
            builder.setContentText(text)
            manager.notify(notificationTag, notificationId, silence(builder.build()))
            time = currentTime
        }
    }

    fun cancel() {
        isCancelled = true
        manager.cancel(notificationTag, notificationId)
    }

    private fun complete(intent: Intent, title: String, text: String, icon: Int) {
        val manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val resultPendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.WORKER.COMPLETION_NOTIFICATION_CHANNEL,
                "Decompile complete notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, Constants.WORKER.COMPLETION_NOTIFICATION_CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(resultPendingIntent)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setAutoCancel(true)
        manager.notify(
            packageName,
            Constants.WORKER.COMPLETED_NOTIFICATION_ID,
            builder.build()
        )

    }

    fun error() {
        val intent = Intent(context, ContainerActivity::class.java)
        intent.putExtra("packageInfo", PackageInfo.fromFile(context, packageFile))
        intent.putExtra("fragmentClass", DecompilerFragment::class.java.name)
        complete(
            intent,
            context.getString(R.string.errorDecompilingApp, packageLabel),
            context.getString(R.string.tapToRetry),
            R.drawable.ic_stat_error
        )
    }

    fun lowMemory(decompiler: String) {
        val intent = Intent(context, ContainerActivity::class.java)
        val packageInfo = PackageInfo.fromFile(context, packageFile)
        intent.putExtra("packageInfo", packageInfo)
        intent.putExtra("fragmentClass", LowMemoryFragment::class.java.name)
        intent.putExtra("decompiler", decompiler)
        complete(
            intent,
            context.getString(R.string.errorDecompilingApp, packageLabel),
            context.getString(R.string.lowMemoryStatusInfo),
            R.drawable.ic_stat_error
        )
    }

    fun success() {
        val intent = Intent(context, ContainerActivity::class.java)
        intent.putExtra("fragmentClass", NavigatorFragment::class.java.name)
        intent.putExtra("selectedApp", SourceInfo.from(
            sourceDir(
                packageName
            )
        ))
        complete(
            intent,
            context.getString(R.string.appHasBeenDecompiled, packageLabel),
            context.getString(R.string.tapToViewSource),
            R.drawable.ic_stat_code
        )
    }
}