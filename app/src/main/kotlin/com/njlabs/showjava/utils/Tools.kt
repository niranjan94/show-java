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

import android.content.Context
import android.content.pm.PackageInfo
import android.net.ConnectivityManager
import android.os.Build
import java.security.MessageDigest
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

private val NON_LATIN = Pattern.compile("[^\\w-]")
private val WHITESPACE = Pattern.compile("[\\s]")

fun toClassName(packageName: String): String {
    return "L" + packageName.trim().replace(".", "/")
}

fun toSlug(input: String): String {
    val noWhiteSpace = WHITESPACE.matcher(input).replaceAll("-")
    val normalized = Normalizer.normalize(noWhiteSpace, Normalizer.Form.NFD)
    val slug = NON_LATIN.matcher(normalized).replaceAll("")
    return slug.toLowerCase(Locale.ENGLISH)
}

fun hashString(type: String, input: String): String {
    val hexChars = "0123456789ABCDEF"
    val bytes = MessageDigest
        .getInstance(type)
        .digest(input.toByteArray())
    val result = StringBuilder(bytes.size * 2)

    bytes.forEach {
        val i = it.toInt()
        result.append(hexChars[i shr 4 and 0x0f])
        result.append(hexChars[i and 0x0f])
    }

    return result.toString()
}

fun checkDataConnection(context: Context): Boolean {
    val connectivityMgr =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return (connectivityMgr.activeNetworkInfo != null &&
            connectivityMgr.activeNetworkInfo.isAvailable &&
            connectivityMgr.activeNetworkInfo.isConnected)
}

fun getDate(): String {
    val date = Date(System.currentTimeMillis())
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(date)
}

/**
 * Get either version name or version code form [packageInfo].
 */
fun getVersion(packageInfo: PackageInfo): String {
    return if (packageInfo.versionName != null)
        packageInfo.versionName
    else
        getVersionCode(packageInfo).toString()
}

/**
 * Get version code from [packageInfo] using the correct method depending on Android version.
 */
fun getVersionCode(packageInfo: PackageInfo): Number {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        packageInfo.longVersionCode
    else
        packageInfo.versionCode
}