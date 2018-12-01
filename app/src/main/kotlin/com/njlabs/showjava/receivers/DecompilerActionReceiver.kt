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

package com.njlabs.showjava.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.njlabs.showjava.Constants
import com.njlabs.showjava.utils.ProcessNotifier
import timber.log.Timber

class DecompilerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Constants.WORKER.ACTION.STOP -> {
                Timber.d("[cancel-request] ID: ${intent.getStringExtra("id")}")
                val id = intent.getStringExtra("id")
                ProcessNotifier(context!!, id).cancel()
                WorkManager.getInstance().cancelUniqueWork(id)
            }
            else -> {
                Timber.i("Received an unknown action.")
            }
        }
    }

}