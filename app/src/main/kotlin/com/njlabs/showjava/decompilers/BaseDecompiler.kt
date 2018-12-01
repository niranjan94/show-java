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
import com.njlabs.showjava.utils.ProcessNotifier
import com.njlabs.showjava.utils.appStorage
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

    protected val decompiler = data.getString("decompiler")

    protected val packageName: String = data.getString("name").toString()
    protected val packageLabel: String = data.getString("label").toString()

    protected val workingDirectory: File = appStorage.resolve("sources/$packageName/")
    protected val cacheDirectory: File = appStorage.resolve("sources/.cache/")

    protected val inputPackageFile: File = File(data.getString("inputPackageFile"))

    protected val outputDexFile: File = workingDirectory.resolve("classes.dex")
    protected val outputJarFile: File = workingDirectory.resolve("$packageName.jar")
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
        onStopped(false)
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
    protected fun buildNotification(title: String) {
        processNotifier = ProcessNotifier(context, id)
            .buildFor(title, packageName, packageLabel, inputPackageFile)
    }

    /**
     * Cancel notification on worker stop
     */
    open fun onStopped(cancelled: Boolean) {
        Timber.d("[cancel-request] cancelled: $cancelled")
        if (cancelled) {
            FileUtils.deleteQuietly(workingDirectory)
        }
        processNotifier?.cancel()
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