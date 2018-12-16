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

import android.content.SharedPreferences
import com.google.ads.consent.ConsentStatus

class UserPreferences(private val prefs: SharedPreferences) {

    companion object {
        const val NAME = "user_preferences"
    }

    interface DEFAULTS {
        companion object {
            const val DARK_MODE = false
            const val SHOW_MEMORY_USAGE = true
            const val SHOW_SYSTEM_APPS = false
            const val MEMORY_THRESHOLD = 80
            const val IGNORE_LIBRARIES = true
            const val CHUNK_SIZE = 500
            const val MAX_ATTEMPTS = 2
        }
    }

    val ignoreLibraries: Boolean
        get() = prefs.getBoolean("ignoreLibraries", DEFAULTS.IGNORE_LIBRARIES)

    val darkMode: Boolean
        get() = prefs.getBoolean("darkMode", DEFAULTS.DARK_MODE)

    val showMemoryUsage: Boolean
        get() = prefs.getBoolean("showMemoryUsage", DEFAULTS.SHOW_MEMORY_USAGE)

    val showSystemApps: Boolean
        get() = prefs.getBoolean("showSystemApps", DEFAULTS.SHOW_SYSTEM_APPS)

    val chunkSize: Int
        get() = prefs.getString("chunkSize", DEFAULTS.CHUNK_SIZE.toString())?.toInt()
                ?: DEFAULTS.CHUNK_SIZE

    val maxAttempts: Int
        get() = prefs.getString("maxAttempts", DEFAULTS.MAX_ATTEMPTS.toString())?.toInt()
                ?: DEFAULTS.MAX_ATTEMPTS

    val memoryThreshold: Int
        get() = prefs.getString("memoryThreshold", DEFAULTS.MEMORY_THRESHOLD.toString())?.toInt()
                ?: DEFAULTS.MEMORY_THRESHOLD

    val consentStatus: Int
        get() = prefs.getInt("consentStatus", ConsentStatus.UNKNOWN.ordinal)
}