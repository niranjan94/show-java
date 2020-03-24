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

package com.njlabs.showjava.utils

import android.app.Application
import android.content.Context
import timber.log.Timber

object StethoUtils {
    fun install(application: Application) {
        try {
            val stethoClazz = Class.forName("com.facebook.stetho.Stetho")
            val method = stethoClazz.getMethod("initializeWithDefaults", Context::class.java)
            method.invoke(null, application)
        } catch (e: Exception) {
            Timber.d(e)
        }
    }
}