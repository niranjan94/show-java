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

package com.njlabs.showjava.utils

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.njlabs.showjava.Constants

class Notifier(
    private val notificationManager: NotificationManager,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationId: Int = Constants.WORKER.PROGRESS_NOTIFICATION_ID,
    private val notificationTag: String?
) {
    var time: Long = 0
    var isCancelled: Boolean = false

    fun updateTitle(title: String) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && currentTime - time >= 500) {
            notificationBuilder.setContentTitle(title)
            notificationBuilder.setProgress(0, 0, true)
            notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
            time = currentTime
        }
    }

    fun updateText(text: String) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && currentTime - time >= 500) {
            notificationBuilder.setContentText(text)
            notificationBuilder.setProgress(0, 0, true)
            notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
            time = currentTime
        }
    }

    fun updateTitleText(title: String, text: String) {
        val currentTime = System.currentTimeMillis()
        if (!isCancelled && currentTime - time >= 500) {
            notificationBuilder.setContentTitle(title)
            notificationBuilder.setContentText(text)
            notificationBuilder.setProgress(0, 0, true)
            notificationManager.notify(notificationTag, notificationId, notificationBuilder.build())
            time = currentTime
        }
    }

    fun cancel() {
        isCancelled = true
        notificationManager.cancel(notificationTag, notificationId)
    }
}