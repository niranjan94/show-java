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

package com.njlabs.showjava.utils.secure

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.github.javiersantos.piracychecker.*
import com.github.javiersantos.piracychecker.enums.InstallerID
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.utils.RequestQueue
import com.njlabs.showjava.utils.SingletonHolder
import com.securepreferences.SecurePreferences
import io.michaelrocks.paranoid.Obfuscate
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.json.JSONObject
import org.solovyev.android.checkout.Billing
import org.solovyev.android.checkout.Purchase
import timber.log.Timber
import java.security.MessageDigest


@Obfuscate
class SecureUtils(val context: Context) {

    private val packageName = "com.njlabs.showjava"
    private val backendUrl = BuildConfig.BACKEND_URL
    private var hasPurchasedPro: Boolean? = null
    private var preferences: SecurePreferences? = null

    val iapProductId = BuildConfig.IAP_PRODUCT_ID

    val purchaseVerifierPath = BuildConfig.PURCHASE_VERIFIER_PATH

    private fun getPreferences(): SecurePreferences {
        if (preferences == null) {
            preferences = SecurePreferences(context)
        }
        return preferences as SecurePreferences
    }

    fun isSafeExtended(allow: (() -> Unit), doNotAllow: (() -> Unit), onError: (() -> Unit)) {
        Timber.d("[pa] isSafeExtended")
        context.piracyChecker {
            enableGooglePlayLicensing(BuildConfig.PLAY_LICENSE_KEY)
            if (BuildConfig.EXTENDED_VALIDATION) {
                enableInstallerId(InstallerID.GOOGLE_PLAY)
                enableUnauthorizedAppsCheck(true)
                enableDebugCheck(true)
            }
            callback {
                doNotAllow { a, b ->
                    Timber.d("[isSafeExtended][doNotAllow] ${a.name} $b")
                    doNotAllow()
                }
                allow { allow() }
                onError {
                    Timber.d("[isSafeExtended][doNotAllow] ${it.name}")
                    onError()
                }
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

    fun hasPurchasedPro(): Boolean {
        if (hasPurchasedPro != null) {
            return hasPurchasedPro as Boolean
        }
        return getPreferences().getBoolean(iapProductId, false)
    }

    fun isPurchaseValid(purchase: Purchase, jsonObject: JSONObject): Boolean {
        if (jsonObject.has("isPurchased") && jsonObject.has("orderId")) {
            return jsonObject.getBoolean("isPurchased") && jsonObject.getString("orderId") == purchase.orderId
        }
        return false
    }

    fun onPurchaseComplete(purchase: Purchase) {
        hasPurchasedPro = true
        getPreferences().edit().putBoolean(purchase.sku, true).commit()
    }

    fun onPurchaseRevert() {
        hasPurchasedPro = false
        getPreferences().edit().putBoolean(iapProductId, false).commit()
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

    /**
     * Make a JSON Request to the backend and return and observable
     */
    fun makeJsonRequest(requestPath: String, payload: Map<String, String>): Observable<JSONObject> {
        return Observable.create { emitter: ObservableEmitter<JSONObject> ->
            val jsonBody = JSONObject()
            var backendUrl = this.backendUrl
            var path = requestPath
            if (!backendUrl.endsWith("/")) {
                backendUrl += "/"
            }
            if (path.startsWith("/")) {
                path = path.removePrefix("/")
            }
            payload.entries.forEach {
                jsonBody.put(it.key, it.value)
            }
            val request = JsonObjectRequest(
                backendUrl + path,
                jsonBody,
                Response.Listener<JSONObject> {
                    emitter.onNext(it)
                    emitter.onComplete()
                }, Response.ErrorListener {
                    emitter.onError(it)
                    emitter.onComplete()
                }
            )
            RequestQueue.getInstance(context).addToRequestQueue(request)
        }
    }

    companion object : SingletonHolder<SecureUtils, Context>(::SecureUtils)
}