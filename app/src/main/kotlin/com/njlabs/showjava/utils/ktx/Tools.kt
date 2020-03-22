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

import android.content.Context
import android.content.pm.PackageInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import java.security.MessageDigest
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

private val NON_LATIN = Pattern.compile("[^\\w-]")
private val WHITESPACE = Pattern.compile("[\\s]")

/**
 * Convert a [packageName] in dot.notation to a class reference (Eg. Lclass/reference)
 */
fun toClassName(packageName: String): String {
    return "L" + packageName.trim().replace(".", "/")
}

/**
 * Convert the [input] to a slug.
 */
fun toSlug(input: String): String {
    val noWhiteSpace = WHITESPACE.matcher(input).replaceAll("-")
    val normalized = Normalizer.normalize(noWhiteSpace, Normalizer.Form.NFD)
    val slug = NON_LATIN.matcher(normalized).replaceAll("")
    return slug.toLowerCase(Locale.ENGLISH)
}

/**
 * Hash the given [input] string using the specified [algorithm]
 */
fun hashString(algorithm: String, input: String): String {
    val hexChars = "0123456789ABCDEF"
    val bytes = MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
    val result = StringBuilder(bytes.size * 2)
    bytes.forEach {
        val i = it.toInt()
        result.append(hexChars[i shr 4 and 0x0f])
        result.append(hexChars[i and 0x0f])
    }
    return result.toString()
}

/**
 * Check if the device is connected to a network.
 */
fun checkDataConnection(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION") val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION") return (activeNetworkInfo.isAvailable && activeNetworkInfo.isConnected)
    }
}

/**
 * Get the current date as ISO 8601 [String]
 */
fun getDate(): String {
    val date = Date(System.currentTimeMillis())
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(date)
}

/**
 * Request JVM to run the garbage collector.
 * Don't think this would do much good. But okay.
 */
fun cleanMemory() {
    Runtime.getRuntime().gc()
    Thread.sleep(500)
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
        @Suppress("DEPRECATION")
        packageInfo.versionCode
}

/**
 * Create a [Bundle] from vararg pairs
 */
fun bundleOf(vararg pairs: Pair<String, Any?>) =
    Bundle(pairs.size).apply {
        for ((key, value) in pairs) {
            putSmart(key, value)
        }
    }
