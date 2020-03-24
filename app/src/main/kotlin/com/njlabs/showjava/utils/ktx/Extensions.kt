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

package com.njlabs.showjava.utils.ktx

import android.graphics.*
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import java.io.File
import java.io.InputStream
import java.io.Serializable
import kotlin.math.max

/**
 * Convert an [InputStream] to a file given a path as a [String]
 */
fun InputStream.toFile(path: String) {
    toFile(File(path))
}

/**
 * Convert an [InputStream] to a file given the destination [File]
 */
fun InputStream.toFile(file: File) {
    file.outputStream().use { this.copyTo(it) }
}

/**
 * Convert a [Map] to a bundle by iterating through the map entries
 */
fun <V> Map<String, V>.toBundle(bundle: Bundle = Bundle()): Bundle = bundle.apply {
    forEach {
        val k = it.key
        val v = it.value
        bundle.putSmart(k, v)
    }
}

/**
 * Put to [Bundle] using the right method based on the type
 */
fun Bundle.putSmart(k: String, v: Any?) = when (v) {
    is IBinder -> {
        putBinder(k, v)
    }
    is Bundle -> putBundle(k, v)
    is Byte -> putByte(k, v)
    is ByteArray -> putByteArray(k, v)
    is String -> putString(k, v)
    is Char -> putChar(k, v)
    is CharArray -> putCharArray(k, v)
    is CharSequence -> putCharSequence(k, v)
    is Float -> putFloat(k, v)
    is FloatArray -> putFloatArray(k, v)
    is Parcelable -> putParcelable(k, v)
    is Serializable -> putSerializable(k, v)
    is Short -> putShort(k, v)
    is ShortArray -> putShortArray(k, v)
    is Boolean -> putBoolean(k, v)
    is Int -> putInt(k, v)
    is IntArray -> putIntArray(k, v)
    else -> {
    }
}

/**
 * Get a circular [Bitmap] from the original
 *
 * Borrowed from: https://stackoverflow.com/a/46613094/1562480 (Yuriy Seredyuk)
 */
fun Bitmap.getCircularBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
    // circle configuration
    val circlePaint = Paint().apply { isAntiAlias = true }
    val circleRadius = max(width, height) / 2f

    // output bitmap
    val outputBitmapPaint =
        Paint(circlePaint).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
    val outputBounds = Rect(0, 0, width, height)
    val output = Bitmap.createBitmap(width, height, config)

    return Canvas(output).run {
        drawCircle(circleRadius, circleRadius, circleRadius, circlePaint)
        drawBitmap(this@getCircularBitmap, outputBounds, outputBounds, outputBitmapPaint)
        output
    }
}