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
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.njlabs.showjava.decompilers.BaseDecompiler
import com.njlabs.showjava.decompilers.JarExtractionWorker
import com.njlabs.showjava.decompilers.JavaExtractionWorker
import com.njlabs.showjava.decompilers.ResourcesExtractionWorker
import timber.log.Timber

class DecompilerWorker(val context: Context, private val params: WorkerParameters) : Worker(context, params) {

    private var worker: BaseDecompiler? = null

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
        var result = Result.FAILURE
        worker ?.let {
            try {
                result = it.doWork()
            } catch (e: Exception) {
                Timber.e(e)
            }
            it.onStopped(false)
        }
        return result
    }

    override fun onStopped(cancelled: Boolean) {
        super.onStopped(cancelled)
        if (worker != null) {
            return worker!!.onStopped(cancelled)
        }
    }
}
