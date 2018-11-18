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
import androidx.work.*
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.utils.Notifier
import com.njlabs.showjava.utils.streams.ProgressStream
import com.njlabs.showjava.workers.DecompilerWorker
import timber.log.Timber
import java.io.File
import java.io.PrintStream
import java.lang.Exception
import java.util.*

abstract class BaseDecompiler(val context: Context, val data: Data) {
    var printStream: PrintStream? = null

    private var id = data.getString("id")
    private var processNotifier: Notifier? = null

    protected val decompiler = data.getString("decompiler")

    protected val packageName = data.getString("name")
    private val packageLabel = data.getString("label")

    protected val workingDirectory: File = File(Environment.getExternalStorageDirectory().canonicalPath, "show-java/sources/$packageName/")
    protected val inputPackageFile: File = File(data.getString("inputPackageFile"))

    protected val outputDexFile: File = File(workingDirectory, "classes.dex")
    protected val outputJarFile: File = File(workingDirectory, "$packageName.jar")
    protected val outputJavaSrcDirectory: File = File(workingDirectory, "src/java")
    protected val outputResSrcDirectory: File = File(workingDirectory, "src/res")

    init {
        printStream = PrintStream(ProgressStream())
        System.setErr(printStream)
        System.setOut(printStream)
    }

    open fun doWork(): ListenableWorker.Result {
        outputJavaSrcDirectory.mkdirs()
        outputResSrcDirectory.mkdirs()
        return ListenableWorker.Result.SUCCESS
    }

    protected fun sendStatus(title: String, message: String) {
        processNotifier?.updateTitleText(title, message)
    }

    protected fun sendStatus(title: String) {
        sendStatus(title, "")
    }

    protected fun exit(exception: Exception?): ListenableWorker.Result {
        Timber.e(exception)
        processNotifier?.cancel()
        return ListenableWorker.Result.FAILURE
    }


    /**
     * Build a persistent notification
     */
    protected fun buildNotification(title: String): Notification {
        val stopIntent = Intent(context, DecompilerActivity::class.java)
        stopIntent.action = Constants.ACTION.STOP_PROCESS
        stopIntent.putExtra("id", id)
        stopIntent.putExtra("packageFilePath", inputPackageFile.canonicalFile)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntentForStop = PendingIntent.getService(context, 0, stopIntent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =  NotificationChannel(
                Constants.PROCESS_NOTIFICATION_CHANNEL_ID,
                "Decompiler notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, Constants.PROCESS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(packageLabel)
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_code_black)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .addAction(R.drawable.ic_stop_black, "Stop decompiler", pendingIntentForStop)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(0, 0, true)

        val notification = builder.build()
        notificationManager.notify(id, Constants.PROCESS_NOTIFICATION_ID, notification)
        processNotifier = Notifier(notificationManager, builder, Constants.PROCESS_NOTIFICATION_ID, id)
        return notification
    }

    open fun onStopped(cancelled: Boolean) {
        processNotifier?.cancel()
    }

    companion object {

        fun formData(dataMap: Map<String, Any>): Data {
            val id = UUID.randomUUID().toString()
            return Data.Builder()
                .putAll(dataMap)
                .putString("id", id)
                .build()
        }

        fun start(dataMap: Map<String, Any>): String {

            val data = formData(dataMap)
            val id = data.getString("id")!!

            val jarExtractionWork = OneTimeWorkRequestBuilder<DecompilerWorker>()
                .addTag("decompile")
                .addTag(id)
                .setInputData(data)
                .build()

            val javaExtractionWork = OneTimeWorkRequestBuilder<DecompilerWorker>()
                .addTag("decompile")
                .addTag(id)
                .setInputData(data)
                .build()

            val resourcesExtractionWork = OneTimeWorkRequestBuilder<DecompilerWorker>()
                .addTag("decompile")
                .addTag(id)
                .setInputData(data)
                .build()

            WorkManager.getInstance()
                .beginUniqueWork(dataMap["name"] as String, ExistingWorkPolicy.KEEP, jarExtractionWork)
                .then(javaExtractionWork)
                .then(resourcesExtractionWork)
                .enqueue()
            return id
        }
    }
}