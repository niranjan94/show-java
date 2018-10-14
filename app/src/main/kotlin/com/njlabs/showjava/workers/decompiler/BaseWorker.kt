package com.njlabs.showjava.workers.decompiler

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.utils.Notifier
import com.njlabs.showjava.utils.streams.ProgressStream
import com.njlabs.showjava.workers.ProcessorService
import java.io.File
import java.io.PrintStream

abstract class BaseWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    var printStream: PrintStream? = null

    protected var data = params.inputData
    private var startID = data.getInt("startID", 0)
    private var processNotifier: Notifier? = null

    protected val decompiler = data.getString("decompiler")

    protected val packageName = data.getString("packageName")
    protected val packageLabel = data.getString("packageLabel")

    protected val workingDirectory: File = File(Environment.getExternalStorageDirectory().canonicalPath, "sources/$packageName/")
    protected val inputPackageFile: File = File(data.getString("inputPackageFile"))

    protected val outputDexFile: File = File(workingDirectory, "classes.dex")
    protected val outputJarFile: File = File(workingDirectory, "$packageName.jar")
    protected val outputJavaSrcDirectory: File = File(workingDirectory, "src/java")
    protected val outputResSrcDirectory: File = File(workingDirectory, "src/res")

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    protected fun sendStatus(tag: String, message: String?) {}
    protected fun sendStatus(tag: String) { sendStatus(tag, null) }

    /**
     * Build a persistent notification
     */
    protected fun buildNotification(): Notification {
        val stopIntent = Intent(context, ProcessorService::class.java)
        stopIntent.action = Constants.ACTION.STOP_PROCESS
        stopIntent.putExtra("startID", startID)

        val pendingIntentForStop = PendingIntent.getService(context, 0, stopIntent, 0)
        val builder = NotificationCompat.Builder(context, "primary-channel")
            .setContentTitle("Decompiling")
            .setContentText("Processing the apk")
            .setSmallIcon(R.drawable.ic_code_black)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .addAction(R.drawable.ic_stop_black, "Stop decompiler", pendingIntentForStop)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(0, 0, true)

        val notification = builder.build()
        notificationManager.notify(Constants.PROCESS_NOTIFICATION_ID, notification)
        processNotifier = Notifier(notificationManager, builder, Constants.PROCESS_NOTIFICATION_ID)
        return notification
    }


}
