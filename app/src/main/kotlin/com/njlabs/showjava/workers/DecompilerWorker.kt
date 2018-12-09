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

package com.njlabs.showjava.workers

import android.content.Context
import androidx.work.Data
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.njlabs.showjava.decompilers.BaseDecompiler
import com.njlabs.showjava.decompilers.JarExtractionWorker
import com.njlabs.showjava.decompilers.JavaExtractionWorker
import com.njlabs.showjava.decompilers.ResourcesExtractionWorker
import com.njlabs.showjava.utils.ProcessNotifier
import com.njlabs.showjava.utils.UserPreferences
import timber.log.Timber
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DecompilerWorker(val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    private var worker: BaseDecompiler? = null
    private val maxAttempts = params.inputData.getInt("maxAttempts", UserPreferences.DEFAULTS.MAX_ATTEMPTS)

    private val id: String = params.inputData.getString("id").toString()
    private val packageName: String = params.inputData.getString("name").toString()
    private val packageLabel: String = params.inputData.getString("label").toString()
    private val inputPackageFile: File = File(params.inputData.getString("inputPackageFile"))
    private val decompilerExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        if (tags.contains("jar-extraction")) {
            worker = JarExtractionWorker(context, params.inputData)
        }
        if (tags.contains("java-extraction")) {
            worker = JavaExtractionWorker(context, params.inputData)
        }
        if (tags.contains("resources-extraction")) {
            worker = ResourcesExtractionWorker(context, params.inputData)
        }
    }

    override fun doWork(): Result {
        var result = if (runAttemptCount >= (maxAttempts - 1)) Result.FAILURE else Result.RETRY
        var ranOutOfMemory = false
        val notifier = ProcessNotifier(context, id)
            .withPackageInfo(packageName, packageLabel, inputPackageFile)

        worker ?.let {
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
            it.onStopped(false)
        }

        if (ranOutOfMemory) {
            result = Result.FAILURE
        }

        if (result == Result.FAILURE) {
            try {
                if (ranOutOfMemory) {
                    notifier.lowMemory()
                } else {
                    notifier.error()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        return result
    }

    override fun onStopped(cancelled: Boolean) {
        super.onStopped(cancelled)
        if (worker != null) {
            return worker!!.onStopped(cancelled)
        }
    }

    companion object {
        fun cancel(context: Context, id: String) {
            ProcessNotifier(context, id).cancel()
            WorkManager.getInstance().cancelUniqueWork(id)
        }
    }
}
