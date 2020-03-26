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

package com.njlabs.showjava.workers

import android.content.Context
import androidx.work.Data
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import com.njlabs.showjava.extractors.BaseExtractor
import com.njlabs.showjava.extractors.JarExtractor
import com.njlabs.showjava.extractors.JavaExtractor
import com.njlabs.showjava.extractors.ResourcesExtractor
import com.njlabs.showjava.utils.ProcessNotifier
import com.njlabs.showjava.utils.UserPreferences
import timber.log.Timber
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A wrapper for each of the 3 extractors to be able to use with Android WorkManager
 */
class DecompilerWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {

    private var worker: BaseExtractor? = null
    private lateinit var step: String
    private val maxAttempts =
        params.inputData.getInt("maxAttempts", UserPreferences.DEFAULTS.MAX_ATTEMPTS)

    private val id: String = params.inputData.getString("id").toString()
    private val packageName: String = params.inputData.getString("name").toString()
    private val packageLabel: String = params.inputData.getString("label").toString()
    private val decompiler: String = params.inputData.getString("decompiler").toString()
    private val chunkSize: Int =
        params.inputData.getInt("chunkSize", UserPreferences.DEFAULTS.CHUNK_SIZE)
    private val memoryThreshold: Int =
        params.inputData.getInt("memoryThreshold", UserPreferences.DEFAULTS.MEMORY_THRESHOLD)
    private val inputPackageFile: File = File(params.inputData.getString("inputPackageFile")!!)

    private val decompilerExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Initialize the appropriate extractor based on the current step of the job
     */
    init {
        if (tags.contains("jar-extraction")) {
            step = "jar-extraction"
            worker = JarExtractor(
                context,
                params.inputData
            )
        }
        if (tags.contains("java-extraction")) {
            step = "java-extraction"
            worker = JavaExtractor(
                context,
                params.inputData
            )
        }
        if (tags.contains("resources-extraction")) {
            step = "resources-extraction"
            worker = ResourcesExtractor(
                context,
                params.inputData
            )
        }
    }

    /**
     * Execute the extractor. Also handles
     *
     * - Retries based on the set max attempts and failure status.
     * - Stopping of the job when memory usage reaches the given threshold
     *
     * Every extractor is run within an executor thread. This allows us to force kill the thread
     * via [ExecutorService.shutdownNow] method when memory usage exceeds set threshold.
     *
     */
    override fun doWork(): Result {
        var result = if (runAttemptCount >= (maxAttempts - 1)) Result.failure() else Result.retry()
        var ranOutOfMemory = false
        val notifier = ProcessNotifier(context, id)
            .withPackageInfo(packageName, packageLabel, inputPackageFile)

        Crashlytics.setString("decompilation_step", step)
        Crashlytics.setString("decompilation_decompiler", decompiler)
        Crashlytics.setString("decompilation_package_name", packageName)
        Crashlytics.setString("decompilation_package_label", packageLabel)
        Crashlytics.setInt("decompilation_chunk_size", chunkSize)
        Crashlytics.setInt("decompilation_memory_threshold", memoryThreshold)

        var outputData = Data.Builder().build()

        worker?.let {
            try {
                val latch = CountDownLatch(1)
                decompilerExecutor.execute {
                    result = it.withNotifier(notifier)
                        .withLowMemoryCallback { isLowMemory ->
                            ranOutOfMemory = isLowMemory
                            outputData = Data.Builder()
                                .putBoolean("ranOutOfMemory", isLowMemory)
                                .build()

                            if (isLowMemory) {
                                latch.countDown()
                                decompilerExecutor.shutdownNow()
                            }
                        }
                        .withAttempt(runAttemptCount)
                    latch.countDown()
                }
                latch.await()
                decompilerExecutor.shutdownNow()
                decompilerExecutor.awaitTermination(2, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Timber.e(e)
            }
            it.onStopped()
        }

        if (ranOutOfMemory) {
            result = Result.failure(outputData)
        }

        if (result == Result.failure()) {
            try {
                if (ranOutOfMemory) {
                    notifier.lowMemory(decompiler)
                } else {
                    notifier.error()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        Crashlytics.setString("decompilation_step", "")
        Crashlytics.setString("decompilation_decompiler", "")
        Crashlytics.setString("decompilation_package_name", "")
        Crashlytics.setString("decompilation_package_label", "")
        Crashlytics.setInt("decompilation_chunk_size", -1)
        Crashlytics.setInt("decompilation_memory_threshold", -1)

        return result
    }

    /**
     * Called when the job is stopped. (Either by user-initiated cancel or when complete)
     * We clean up the notifications and caches if any on shutdown.
     *
     */
    override fun onStopped() {
        super.onStopped()
        worker?.onStopped()
    }

    companion object {
        /**
         * A helper method to cancel a decompilation job by packageName ([id])
         */
        fun cancel(context: Context, id: String) {
            ProcessNotifier(context, id).cancel()
            WorkManager.getInstance(context).cancelUniqueWork(id)
        }
    }
}
