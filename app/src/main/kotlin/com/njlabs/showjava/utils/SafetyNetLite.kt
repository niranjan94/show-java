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
import com.github.javiersantos.piracychecker.*
import com.github.javiersantos.piracychecker.enums.InstallerID
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.SingletonHolder
import com.securepreferences.SecurePreferences
import io.michaelrocks.paranoid.Obfuscate
import org.solovyev.android.checkout.Billing
import timber.log.Timber
import java.security.MessageDigest


@Obfuscate
class SafetyNetLite(val context: Context) {

    private val packageName = "com.njlabs.showjava"
    private var hasPurchasedPro: Boolean? = null
    private var preferences: SecurePreferences? = null

    private fun getPreferences(): SecurePreferences {
        if (preferences == null) {
            preferences = SecurePreferences(context)
        }
        return preferences as SecurePreferences
    }

    fun isSafeExtended(allow: (() -> Unit), doNotAllow: (() -> Unit), onError: (() -> Unit)) {
        context.piracyChecker {
            enableGooglePlayLicensing(BuildConfig.PLAY_LICENSE_KEY)
            if (BuildConfig.EXTENDED_VALIDATION) {
                enableInstallerId(InstallerID.GOOGLE_PLAY)
                enableUnauthorizedAppsCheck(true)
                enableDebugCheck(true)
                enableEmulatorCheck()
            }
            saveResultToSharedPreferences(getPreferences(), "is_safe")
            callback {
                doNotAllow { _, _ -> doNotAllow() }
                allow { allow() }
                onError { onError() }
            }
        }.start()
    }

    fun getBilling(): Billing {
        return Billing(context, object : Billing.DefaultConfiguration() {
            override fun getPublicKey(): String {
                return BuildConfig.PLAY_LICENSE_KEY
            }
        })
    }

    fun getIapProductId(): String {
        return BuildConfig.IAP_PRODUCT_ID
    }

    fun hasPurchasedPro(): Boolean {
        if (!this.isSafe()) {
            return false
        }
        if (hasPurchasedPro != null) {
            return hasPurchasedPro as Boolean
        }
        return getPreferences().getBoolean("show-java-pro", false)
    }

    fun onPurchaseComplete() {
        hasPurchasedPro = true
        getPreferences().edit().putBoolean("show-java-pro", true).apply()
    }

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
    fun checkAppSignature() {
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

    private fun isSafe(): Boolean {
        if (context.packageName == packageName) {
            return false
        }

        if (!BuildConfig.EXTENDED_VALIDATION) {
            return true
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

    companion object : SingletonHolder<SafetyNetLite, Context>(::SafetyNetLite)
}