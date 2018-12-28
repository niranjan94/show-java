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
    const val FRAGMENT_TAG = "com.njlabs.showjava.fragments.primary"
    const val FRAGMENT_BACKSTACK = "com.njlabs.showjava.fragments.backstack.primary"

    interface EVENTS {
        companion object {
            const val CLEAR_SOURCE_HISTORY = "clear_source_history"
            const val CHANGE_FONT = "change_font"
            const val TOGGLE_DARK_MODE = "toggle_dark_mode"
            const val SELECT_DECOMPILER = "select_decompiler"
            const val DECOMPILE_APP = "decompile_app"
            const val REPORT_APP_LOW_MEMORY = "report_app_low_memory"
        }
    }

    /**
     * Workers related constants
     */
    interface WORKER {

        companion object {
            const val STATUS_TYPE = "com.njlabs.showjava.worker.STATUS_TYPE"
            const val STATUS_TITLE = "com.njlabs.showjava.worker.STATUS_TITLE"
            const val STATUS_MESSAGE = "com.njlabs.showjava.worker.STATUS_MESSAGE"

            const val PROGRESS_NOTIFICATION_CHANNEL = "com.njlabs.showjava.worker.notification.progress"
            const val COMPLETION_NOTIFICATION_CHANNEL = "com.njlabs.showjava.worker.notification.completion"
            const val PROGRESS_NOTIFICATION_ID = 1094
            const val COMPLETED_NOTIFICATION_ID = 1095
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