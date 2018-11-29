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

package com.njlabs.showjava.decompilers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_SOUND
import androidx.core.app.NotificationCompat.DEFAULT_VIBRATE
import androidx.work.*
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.receivers.DecompilerActionReceiver
import com.njlabs.showjava.utils.Notifier
import com.njlabs.showjava.utils.streams.ProgressStream
import com.njlabs.showjava.workers.DecompilerWorker
import timber.log.Timber
import java.io.File
import java.io.PrintStream
import java.util.*

abstract class BaseDecompiler(val context: Context, val data: Data) {
    var printStream: PrintStream? = null

    private var id = data.getString("id")
    private var processNotifier: Notifier? = null

    protected val decompiler = data.getString("decompiler")

    protected val packageName: String = data.getString("name").toString()
    protected val packageLabel: String = data.getString("label").toString()

    protected val workingDirectory: File = File(
        Environment.getExternalStorageDirectory().canonicalPath,
        "show-java/sources/$packageName/"
    )
    protected val cacheDirectory: File = File(
        Environment.getExternalStorageDirectory().canonicalPath,
        "show-java/.cache/"
    )
    protected val inputPackageFile: File = File(data.getString("inputPackageFile"))

    protected val outputDexFile: File = File(workingDirectory, "classes.dex")
    protected val outputJarFile: File = File(workingDirectory, "$packageName.jar")
    protected val outputJavaSrcDirectory: File = File(workingDirectory, "src/java")
    protected val outputResSrcDirectory: File = File(workingDirectory, "src/res")

    init {
        printStream = PrintStream(ProgressStream(this))
        System.setErr(printStream)
        System.setOut(printStream)
    }

    /**
     * Prepare the required directories.
     * All child classes must call this method on override.
     */
    open fun doWork(): ListenableWorker.Result {
        outputJavaSrcDirectory.mkdirs()
        outputResSrcDirectory.mkdirs()
        return ListenableWorker.Result.SUCCESS
    }

    /**
     * Update the notification and broadcast status
     */
    protected fun sendStatus(title: String, message: String) {
        processNotifier?.updateTitleText(title, message)
        this.broadcastStatus(title, message)
    }

    fun sendStatus(message: String) {
        sendStatus(context.getString(R.string.processing), message)
    }

    fun setStep(title: String) {
        sendStatus(title, "")
    }

    /**
     * Clear the notification and exit marking the work job as failed
     */
    protected fun exit(exception: Exception?): ListenableWorker.Result {
        Timber.e(exception)
        processNotifier?.cancel()
        return ListenableWorker.Result.FAILURE
    }

    /**
     * Broadcast the status to the receiver
     */
    private fun broadcastStatus(title: String, message: String) {
        context.sendBroadcast(
            Intent(Constants.WORKER.ACTION.BROADCAST)
                .putExtra(Constants.WORKER.STATUS_KEY, title)
                .putExtra(Constants.WORKER.STATUS_MESSAGE, message)
        )
    }

    /**
     * Build a persistent notification
     */
    protected fun buildNotification(title: String): Notification {
        val stopIntent = Intent(context, DecompilerActionReceiver::class.java)
        stopIntent.action = Constants.WORKER.ACTION.STOP
        stopIntent.putExtra("id", id)
        stopIntent.putExtra("packageFilePath", inputPackageFile.canonicalFile)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntentForStop = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.WORKER.NOTIFICATION_CHANNEL,
                "Decompiler notification",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }

        val actionIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            R.drawable.ic_stop_black else R.drawable.ic_stat_stop

        val builder = NotificationCompat.Builder(context, Constants.WORKER.NOTIFICATION_CHANNEL)
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

        val notification = builder.build()
        notification.sound = null
        notification.vibrate = null
        notification.defaults = notification.defaults and DEFAULT_SOUND.inv()
        notification.defaults = notification.defaults and DEFAULT_VIBRATE.inv()

        notificationManager.notify(id, Constants.WORKER.NOTIFICATION_ID, notification)
        processNotifier =
                Notifier(notificationManager, builder, Constants.WORKER.NOTIFICATION_ID, id)
        return notification
    }

    /**
     * Cancel notification on worker stop
     */
    open fun onStopped(cancelled: Boolean) {
        processNotifier?.cancel()
    }

    companion object {

        /**
         * For the WorkManager compatible Data object from the given map
         */
        fun formData(dataMap: Map<String, Any>): Data {
            val id = UUID.randomUUID().toString()
            return Data.Builder()
                .putAll(dataMap)
                .putString("id", id)
                .build()
        }

        /**
         * Start the jobs using the given map
         */
        fun start(dataMap: Map<String, Any>): String {

            val data = formData(dataMap)
            val id = data.getString("id")!!

            val jarExtractionWork = OneTimeWorkRequestBuilder<DecompilerWorker>()
                .addTag("decompile")
                .addTag(id)
                .addTag(dataMap["name"] as String)
                .setInputData(data)
                .build()

            val javaExtractionWork = OneTimeWorkRequestBuilder<DecompilerWorker>()
                .addTag("decompile")
                .addTag(id)
                .addTag(dataMap["name"] as String)
                .setInputData(data)
                .build()

            val resourcesExtractionWork = OneTimeWorkRequestBuilder<DecompilerWorker>()
                .addTag("decompile")
                .addTag(id)
                .addTag(dataMap["name"] as String)
                .setInputData(data)
                .build()

            WorkManager.getInstance()
                .beginUniqueWork(
                    dataMap["name"] as String,
                    ExistingWorkPolicy.KEEP,
                    jarExtractionWork
                )
                .then(javaExtractionWork)
                .then(resourcesExtractionWork)
                .enqueue()
            return id
        }
    }
}