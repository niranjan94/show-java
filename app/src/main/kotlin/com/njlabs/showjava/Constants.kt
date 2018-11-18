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

package com.njlabs.showjava

object Constants {
    const val PROCESS_BROADCAST_ACTION = "com.njlabs.showjava.process.BROADCAST"
    const val PROCESS_STATUS_KEY = "com.njlabs.showjava.process.STATUS_KEY"
    const val PROCESS_STATUS_MESSAGE = "com.njlabs.showjava.process.STATUS_MESSAGE"
    const val PROCESS_DIR = "com.njlabs.showjava.process.DIR"
    const val PROCESS_PACKAGE_ID = "com.njlabs.showjava.process.PACKAGE_ID"
    const val PROCESS_NOTIFICATION_ID = 1
    const val PROCESS_NOTIFICATION_CHANNEL_ID = "com.njlabs.showjava"
    const val STORAGE_PERMISSION_REQUEST = 3
    const val FILE_PICKER_RESULT = 9600

    interface ACTION {
        companion object {
            const val START_PROCESS = "com.njlabs.showjava.process.action.START"
            const val STOP_PROCESS = "com.njlabs.showjava.process.action.STOP"
            const val STOP_PROCESS_FOR_NEW = "com.njlabs.showjava.process.action.STOP_FOR_NEW"
        }
    }
}