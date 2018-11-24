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

/**
 * Constants used throughout the application
 */
object Constants {

    const val STORAGE_PERMISSION_REQUEST = 1009

    /**
     * Workers related constants
     */
    interface WORKER {
        companion object {
            const val STATUS_KEY = "com.njlabs.showjava.worker.STATUS_KEY"
            const val STATUS_MESSAGE = "com.njlabs.showjava.worker.STATUS_MESSAGE"
            const val NOTIFICATION_CHANNEL = "com.njlabs.showjava"
            const val NOTIFICATION_ID = 1094
        }

        /**
         * Actions used for interacting with workers
         */
        interface ACTION {
            companion object {
                // Action to broadcast status to receivers
                const val BROADCAST = "com.njlabs.showjava.worker.action.BROADCAST"

                // Action to instruct the receiver to stop the worker
                const val STOP = "com.njlabs.showjava.worker.action.STOP"
            }
        }
    }
}