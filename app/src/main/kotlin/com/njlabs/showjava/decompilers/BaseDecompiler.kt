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

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.utils.ProcessNotifier
import com.njlabs.showjava.utils.appStorage
import com.njlabs.showjava.utils.cleanMemory
import com.njlabs.showjava.utils.streams.ProgressStream
import com.njlabs.showjava.workers.DecompilerWorker
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.io.PrintStream

abstract class BaseDecompiler(val context: Context, val data: Data) {
    var printStream: PrintStream? = null

    private var id = data.getString("id")
    private var processNotifier: ProcessNotifier? = null
    private var runAttemptCount: Int = 0

    protected val decompiler = data.getString("decompiler")
    protected val type = PackageInfo.Type.values()[data.getInt("type", 0)]
    private val maxAttempts = data.getInt("maxAttempts", Constants.WORKER.PARAMETERS.MAX_ATTEMPTS)

    protected val packageName: String = data.getString("name").toString()
    protected val packageLabel: String = data.getString("label").toString()

    protected val workingDirectory: File = appStorage.resolve("sources/$packageName/")
    protected val cacheDirectory: File = appStorage.resolve("sources/.cache/")

    protected val inputPackageFile: File = File(data.getString("inputPackageFile"))

    protected val outputDexFiles: File = workingDirectory.resolve("dex-files")
    protected val outputJarFiles: File = workingDirectory.resolve("jar-files")
    protected val outputJavaSrcDirectory: File = workingDirectory.resolve("src/java")
    protected val outputResSrcDirectory: File = workingDirectory.resolve("src/res")

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
        cleanMemory()
        outputJavaSrcDirectory.mkdirs()
        outputResSrcDirectory.mkdirs()
        return ListenableWorker.Result.SUCCESS
    }

    fun withAttempt(attempt: Int = 0): ListenableWorker.Result {
        this.runAttemptCount = attempt
        return this.doWork()
    }

    /**
     * Update the notification and broadcast status
     */
    protected fun sendStatus(title: String, message: String, forceSet: Boolean = false) {
        processNotifier?.updateTitleText(title, message, forceSet)
        this.broadcastStatus(title, message)
    }

    fun sendStatus(message: String, forceSet: Boolean = false) {
        processNotifier?.updateText(message, forceSet)
        this.broadcastStatus(null, message)
    }

    fun setStep(title: String) {
        sendStatus(title, "", true)
    }

    /**
     * Clear the notification and exit marking the work job as failed
     */
    protected fun exit(exception: Exception?): ListenableWorker.Result {
        Timber.e(exception)
        onStopped(false)
        return if (runAttemptCount >= maxAttempts) {
            processNotifier?.error()
            ListenableWorker.Result.FAILURE
        }
        else
            ListenableWorker.Result.RETRY
    }

    /**
     * Broadcast the status to the receiver
     */
    private fun broadcastStatus(title: String?, message: String) {
        context.sendBroadcast(
            Intent(Constants.WORKER.ACTION.BROADCAST + packageName)
                .putExtra(Constants.WORKER.STATUS_KEY, title)
                .putExtra(Constants.WORKER.STATUS_MESSAGE, message)
        )
    }

    /**
     * Build a persistent notification
     */
    protected fun buildNotification(title: String) {
        processNotifier = ProcessNotifier(context, id)
            .buildFor(title, packageName, packageLabel, inputPackageFile)
    }

    fun onCompleted() {
        processNotifier?.success()
        broadcastStatus(
            context.getString(R.string.appHasBeenDecompiled, packageLabel),
            ""
        )
    }

    /**
     * Cancel notification on worker stop
     */
    open fun onStopped(cancelled: Boolean = false) {
        Timber.d("[cancel-request] cancelled: $cancelled")
        processNotifier?.cancel()
        if (cancelled) {
            FileUtils.deleteQuietly(workingDirectory)
        }
    }

    companion object {

        fun isAvailable(decompiler: String): Boolean {
            return when (decompiler) {
                "cfr" -> true
                "jadx" -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                "fernflower" -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                else -> false
            }
        }

        /**
         * For the WorkManager compatible Data object from the given map
         */
        fun formData(dataMap: Map<String, Any>): Data {
            val id = dataMap["name"] as String
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

            fun buildWorkRequest(type: String): OneTimeWorkRequest {
                return OneTimeWorkRequestBuilder<DecompilerWorker>()
                    .addTag("decompile")
                    .addTag(type)
                    .addTag(id)
                    .setInputData(data)
                    .build()
            }

            WorkManager.getInstance()
                .beginUniqueWork(
                    id,
                    ExistingWorkPolicy.REPLACE,
                    buildWorkRequest("jar-extraction")
                )
                .then(buildWorkRequest("java-extraction"))
                .then(buildWorkRequest("resources-extraction"))
                .enqueue()
            return id
        }
    }
}