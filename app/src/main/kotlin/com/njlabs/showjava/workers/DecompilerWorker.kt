package com.njlabs.showjava.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters


abstract class DecompilerWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return Result.SUCCESS
    }
}
