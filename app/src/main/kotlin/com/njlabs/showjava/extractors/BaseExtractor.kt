/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2020 Niranjan Rajendran
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

package com.njlabs.showjava.extractors

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.extractors.decompilers.BaseDecompiler
import com.njlabs.showjava.extractors.decompilers.DecompilerType
import com.njlabs.showjava.extractors.decompilers.getDecompilerInstance
import com.njlabs.showjava.utils.ProcessNotifier
import com.njlabs.showjava.utils.UserPreferences
import com.njlabs.showjava.utils.ktx.Storage
import com.njlabs.showjava.utils.ktx.cleanMemory
import com.njlabs.showjava.utils.streams.ProgressStream
import com.njlabs.showjava.workers.DecompilerWorker
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.io.PrintStream
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The base decompiler. This reads the input [Data] into easy to use properties of the class.
 * All other components of the decompiler will extend this one.
 */
abstract class BaseExtractor(val context: Context, val data: Data) :
    CoroutineScope by CoroutineScope(Dispatchers.IO) {

    val printStream: PrintStream
    val decompiler: BaseDecompiler

    private var id = data.getString("id")
    private var processNotifier: ProcessNotifier? = null
    private var runAttemptCount: Int = 0
    protected var outOfMemory = false

    protected val type = PackageInfo.Type.values()[data.getInt("type", 0)]
    private val maxAttempts = data.getInt("maxAttempts", UserPreferences.DEFAULTS.MAX_ATTEMPTS)
    private val memoryThreshold = data.getInt("memoryThreshold", 80)

    protected val packageName = data.getString("name").toString()
    protected val packageLabel = data.getString("label").toString()

    protected val keepIntermediateFiles =
        data.getBoolean("keepIntermediateFiles", UserPreferences.DEFAULTS.KEEP_INTERMEDIATE_FILES)

    private val appStorage: File by lazy {
        Storage.getInstance(context).appStorage
    }

    protected val workingDirectory: File = appStorage.resolve("sources/$packageName/")
    protected val cacheDirectory: File = appStorage.resolve("sources/.cache/")

    protected val inputPackageFile: File = File(data.getString("inputPackageFile")!!)

    protected val outputDexFiles: File = workingDirectory.resolve("dex-files")
    protected val outputJarFiles: File = workingDirectory.resolve("jar-files")
    protected val outputSrcDirectory: File = workingDirectory.resolve("src")
    protected val outputJavaSrcDirectory: File = outputSrcDirectory.resolve("java")

    private var onLowMemory: ((Boolean) -> Unit)? = null
    private var memoryThresholdCrossCount = 0
    protected open val maxMemoryAdjustmentFactor = 1.3

    init {
        printStream = PrintStream(ProgressStream {
            this.sendStatus(it)
        })

        decompiler = getDecompilerInstance(
            DecompilerType.valueOf(data.getString("decompiler")!!.toUpperCase(Locale.ENGLISH)),
            printStream
        )
        System.setErr(printStream)
        System.setOut(printStream)
    }

    /**
     * Prepare the required directories.
     * All child classes must call this method on override.
     */
    open fun doWork(): ListenableWorker.Result {
        cleanMemory()
        launch {
            monitorMemory()
        }
        outputJavaSrcDirectory.mkdirs()
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            data.keyValueMap.forEach { (t, u) ->
                Timber.d("[WORKER] [INPUT] $t: $u")
            }
        }

        return ListenableWorker.Result.success()
    }

    fun withAttempt(attempt: Int = 0): ListenableWorker.Result {
        this.runAttemptCount = attempt
        return this.doWork()
    }

    fun withNotifier(notifier: ProcessNotifier): BaseExtractor {
        this.processNotifier = notifier
        return this
    }

    fun withLowMemoryCallback(onLowMemory: ((Boolean) -> Unit)): BaseExtractor {
        this.onLowMemory = onLowMemory
        return this
    }

    private suspend fun monitorMemory() {
        withContext(Dispatchers.IO) {
            while (true) {
                val maxAdjusted = Runtime.getRuntime().maxMemory() / maxMemoryAdjustmentFactor
                val used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
                    .freeMemory()).toDouble().let { m ->
                    if (m > maxAdjusted) maxAdjusted else m
                }
                val usedPercentage = (used / maxAdjusted) * 100

                Timber.d("[mem] ----")
                Timber.d("[mem] Used: ${FileUtils.byteCountToDisplaySize(used.toLong())}")
                Timber.d("[mem] Max: ${FileUtils.byteCountToDisplaySize(maxAdjusted.toLong())} at factor $maxMemoryAdjustmentFactor")
                Timber.d("[mem] Used %: $usedPercentage")

                broadcastStatus("memory", "%.2f".format(usedPercentage), "memory")

                if (usedPercentage > memoryThreshold) {
                    if (memoryThresholdCrossCount > 2) {
                        onLowMemory?.invoke(true)
                    } else {
                        memoryThresholdCrossCount++
                    }
                } else {
                    memoryThresholdCrossCount = 0
                }
                delay(1000)
            }
        }
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

    /**
     * Set the current decompilation step
     */
    fun setStep(title: String) {
        sendStatus(title, context.getString(R.string.initializing), true)
    }

    /**
     * Clear the notification and exit marking the work job as failed
     */
    protected fun exit(exception: Exception?): ListenableWorker.Result {
        Timber.e(exception)
        onStopped()
        return if (runAttemptCount >= (maxAttempts - 1))
            ListenableWorker.Result.failure()
        else
            ListenableWorker.Result.retry()
    }

    /**
     * Return a success only if the conditions is true. Else exit with an exception.
     */
    protected fun successIf(condition: Boolean): ListenableWorker.Result {
        cancel()
        return if (condition)
            ListenableWorker.Result.success()
        else
            exit(Exception("Success condition failed"))
    }

    /**
     * Broadcast the status to the receiver
     */
    private fun broadcastStatus(title: String?, message: String, type: String = "progress") {
        context.sendBroadcast(
            Intent(Constants.WORKER.ACTION.BROADCAST + packageName)
                .putExtra(Constants.WORKER.STATUS_TITLE, title)
                .putExtra(Constants.WORKER.STATUS_MESSAGE, message)
                .putExtra(Constants.WORKER.STATUS_TYPE, type)
        )
    }

    /**
     * Build a persistent notification
     */
    protected fun buildNotification(title: String) {
        if (processNotifier == null) {
            processNotifier = ProcessNotifier(context, id)
        }
        processNotifier!!.buildFor(
            title,
            packageName,
            packageLabel,
            inputPackageFile,
            context.resources.getStringArray(R.array.decompilersValues).indexOf(decompiler.name)
        )
    }

    /**
     * Clear notifications and show a success notification.
     */
    fun onCompleted() {
        processNotifier?.success()
        broadcastStatus(
            context.getString(R.string.appHasBeenDecompiled, packageLabel),
            ""
        )
        cancel()
    }

    /**
     * Cancel notification on worker stop
     */
    open fun onStopped() {
        cancel()
        Timber.d("[cancel-request] cancelled")
        processNotifier?.cancel()
    }

    companion object {

        /**
         * Check if the specified decompiler is available on the device based on the android version
         */
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
        fun start(context: Context, dataMap: Map<String, Any>): String {

            val data =
                formData(
                    dataMap
                )
            val id = data.getString("id")!!

            fun buildWorkRequest(type: String): OneTimeWorkRequest {
                return OneTimeWorkRequestBuilder<DecompilerWorker>()
                    .addTag("decompile")
                    .addTag(type)
                    .addTag(id)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 0, TimeUnit.SECONDS)
                    .setInputData(data)
                    .build()
            }

            WorkManager.getInstance(context)
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

    class OutOfMemoryError : Exception()
}