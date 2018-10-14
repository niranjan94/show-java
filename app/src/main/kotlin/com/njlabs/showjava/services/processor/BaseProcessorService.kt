package com.njlabs.showjava.services.processor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.utils.Notifier
import com.njlabs.showjava.utils.Tools
import timber.log.Timber


abstract class BaseProcessorService : Service() {

    protected var processNotifier: Notifier? = null
    protected var startID: Int = 0

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Build a persistent notification
     */
    protected fun buildNotification(): Notification {
        val stopIntent = Intent(this, ProcessorService::class.java)
        stopIntent.action = Constants.ACTION.STOP_PROCESS
        stopIntent.putExtra("startID", startID)

        val pendingIntentForStop = PendingIntent.getService(this, 0, stopIntent, 0)
        val builder = NotificationCompat.Builder(this, "primary-channel")
            .setContentTitle("Decompiling")
            .setContentText("Processing the apk")
            .setSmallIcon(R.drawable.ic_code_black)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .addAction(R.drawable.ic_stop_black, "Stop decompiler", pendingIntentForStop)
            .setOngoing(true)
            .setAutoCancel(false)
            .setProgress(0, 0, true)

        val notification = builder.build()
        notificationManager.notify(Constants.PROCESS_NOTIFICATION_ID, notification)
        processNotifier = Notifier(notificationManager, builder, Constants.PROCESS_NOTIFICATION_ID)
        return notification
    }

    /**
     * Broadcast a key-value pair status info
     */
    fun broadcastStatus(statusKey: String, statusData: String = "") {
        sendNotification(statusKey, statusData)
        sendBroadcast(
            Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                .putExtra(Constants.PROCESS_STATUS_MESSAGE, statusData)
        )
    }

    /**
     * Broadcast a status with the package information
     */
    fun broadcastStatusWithPackageInfo(statusKey: String, dir: String, packId: String) {
        sendNotification(statusKey)
        sendBroadcast(
            Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                .putExtra(Constants.PROCESS_DIR, dir)
                .putExtra(Constants.PROCESS_PACKAGE_ID, packId)
        )
    }

    private fun sendNotification(key: String, data: String = "") {
        // TODO
    }

    /**
     * Kill on-self
     */
    internal fun killSelf(shouldBroadcast: Boolean, startID: Int) {
        if (shouldBroadcast) {
            broadcastStatus("exit")
        }
        stopForeground(true)
        stopSelf(startID)
        try {
            notificationManager.cancel(Constants.PROCESS_NOTIFICATION_ID)
            Tools.forceKillAllProcessorServices(this)
        } catch (e: Exception) {
            Timber.e(e)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            processNotifier?.cancel()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }


}
