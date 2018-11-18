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

package com.njlabs.showjava.utils.streams

import androidx.annotation.NonNull
import timber.log.Timber
import java.io.OutputStream

/**
 * A custom output stream that strips unnecessary stuff from raw input stream
 */
class ProgressStream : OutputStream() {
    override fun write(@NonNull data: ByteArray, offset: Int, length: Int) {
        val str = String(data)
            .replace("\n", "")
            .replace("\r", "")
            .replace("INFO:", "")
            .replace("ERROR:", "")
            .replace("WARN:", "")
            .replace("\n\r", "")
            .replace("... done", "")
            .replace("at", "")
            .trim()
        if (str.isNotEmpty()) {
            Timber.i("[ProgressStream] $str")
        }
    }
    override fun write(byte: Int) {
        // Just a stub. We aren't implementing this.
    }
}