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
import java.text.Normalizer
import java.util.Locale
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

fun checkDataConnection(context: Context): Boolean {
    val connectivityMgr =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return (connectivityMgr.activeNetworkInfo != null &&
            connectivityMgr.activeNetworkInfo.isAvailable &&
            connectivityMgr.activeNetworkInfo.isConnected)
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