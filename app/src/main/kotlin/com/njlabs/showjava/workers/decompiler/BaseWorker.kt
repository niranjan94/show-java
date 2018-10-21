package com.njlabs.showjava.workers.decompiler

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.utils.Notifier
import com.njlabs.showjava.utils.streams.ProgressStream
import timber.log.Timber
import java.io.File
import java.io.PrintStream
import java.lang.Exception
import java.util.*
import android.app.NotificationChannel
import android.os.Build
import com.njlabs.showjava.activities.decompiler.DecompilerActivity


abstract class BaseWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    var printStream: PrintStream? = null

    protected var data = params.inputData
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

    override fun doWork(): Result {
        outputJavaSrcDirectory.mkdirs()
        outputResSrcDirectory.mkdirs()
        return Result.SUCCESS
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
        return Result.FAILURE
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

    override fun onStopped(cancelled: Boolean) {
        super.onStopped(cancelled)
        processNotifier?.cancel()
    }

    companion object {
        fun start(dataMap: Map<String, Any>): String {
            val id = UUID.randomUUID().toString()

            val data = Data.Builder()
                .putAll(dataMap)
                .putString("id", id)
                .build()

            val jarExtractionWork = OneTimeWorkRequestBuilder<JarExtractionWorker>()
                .addTag("decompile")
                .addTag(id)
                .setInputData(data)
                .build()

            val javaExtractionWork = OneTimeWorkRequestBuilder<JavaExtractionWorker>()
                .addTag("decompile")
                .addTag(id)
                .setInputData(data)
                .build()

            val resourcesExtractionWork = OneTimeWorkRequestBuilder<ResourcesExtractionWorker>()
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
