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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Base64
import com.njlabs.showjava.BuildConfig
import io.michaelrocks.paranoid.Obfuscate
import timber.log.Timber
import java.security.MessageDigest

@Obfuscate
class SafetyNetLite {

    companion object {

        const val packageName = "com.njlabs.showjava"

        @SuppressLint("PrivateApi")
        @Throws(Exception::class)
        fun getSystemProperty(name: String): String {
            val systemPropertyClass = Class.forName("android.os.SystemProperties")
            return systemPropertyClass
                .getMethod("get", String::class.java)
                .invoke(systemPropertyClass, name) as String
        }

        @Suppress("DEPRECATION")
        @SuppressLint("PackageManagerGetSignatures")
        fun checkAppSignature(context: Context) {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName, PackageManager.GET_SIGNATURES
            )
            packageInfo.signatures.forEach { signature ->
                val signatureBytes = signature.toByteArray()
                val md = MessageDigest.getInstance("SHA")
                md.update(signatureBytes)
                val currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Timber.d("[currentSignature] %s", currentSignature)
            }
        }

        fun isSafe(context: Context): Boolean {
            if (context.packageName.compareTo(packageName) != 0) {
                return false
            }

            val installer = context.packageManager.getInstallerPackageName(packageName)
            if (installer == null || installer != "com.android.vending") {
                return false
            }

            try {
                val goldfish = getSystemProperty("ro.hardware").contains("goldfish")
                val emu = getSystemProperty("ro.kernel.qemu").isNotEmpty()
                val sdk = getSystemProperty("ro.product.model") == "sdk"
                if (emu || goldfish || sdk) {
                    return false
                }
            } catch (e: Exception) { }

            if (BuildConfig.DEBUG || 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                return false
            }

            return false
        }
    }
}