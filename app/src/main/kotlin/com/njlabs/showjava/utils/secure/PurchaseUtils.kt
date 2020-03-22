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

package com.njlabs.showjava.utils.secure

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.solovyev.android.checkout.*
import timber.log.Timber

@Obfuscate
class PurchaseUtils(
    private val activityContext: BaseActivity,
    val secureUtils: SecureUtils,
    val isLoading: (Boolean) -> Unit = {}
) {

    private var completeCallback: () -> Unit = {}

    lateinit var checkout: ActivityCheckout
    private lateinit var inventory: Inventory
    private var lessVerbose: Boolean = false

    fun doOnComplete(completeCallback: () -> Unit) {
        this.completeCallback = completeCallback
    }

    fun initializeCheckout(
        withPurchaseFlow: Boolean = false,
        lessVerbose: Boolean = false
    ): ActivityCheckout {
        this.lessVerbose = lessVerbose
        checkout = Checkout.forActivity(activityContext, secureUtils.getBilling())
        checkout.start()

        if (withPurchaseFlow) {
            checkout.createPurchaseFlow(PurchaseListener())
        }

        inventory = checkout.makeInventory()
        inventory.load(
            Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(ProductTypes.IN_APP, secureUtils.iapProductId),
            InventoryCallback()
        )

        return checkout
    }

    inner class PurchaseListener : EmptyRequestListener<Purchase>() {
        override fun onSuccess(purchase: Purchase) {
            if (purchase.sku == secureUtils.iapProductId && purchase.state == Purchase.State.PURCHASED) {
                onPurchaseComplete(purchase)
            }
        }

        override fun onError(response: Int, e: Exception) {
            val messageKey = when (response) {
                ResponseCodes.USER_CANCELED -> R.string.errorUserCancelled
                ResponseCodes.ITEM_ALREADY_OWNED -> R.string.errorAlreadyOwned
                ResponseCodes.ITEM_UNAVAILABLE -> R.string.errorItemUnavailable
                ResponseCodes.ACCOUNT_ERROR -> R.string.errorAccount
                ResponseCodes.ERROR -> R.string.errorPayment
                else -> R.string.errorRequest
            }
            isLoading(false)
            Toast.makeText(
                activityContext,
                activityContext.getString(messageKey),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    inner class InventoryCallback : Inventory.Callback {
        override fun onLoaded(products: Inventory.Products) {
            var wasAnyPurchased = false
            products.forEach {
                val isPurchased = it.isPurchased(secureUtils.iapProductId)
                if (isPurchased) {
                    it.getPurchaseInState(
                        secureUtils.iapProductId,
                        Purchase.State.PURCHASED
                    )?.let { purchase ->
                        onPurchaseComplete(purchase)
                        wasAnyPurchased = true
                    }
                }
            }
            if (!wasAnyPurchased) {
                secureUtils.onPurchaseRevert()
            }
        }
    }

    private fun onPurchaseComplete(purchase: Purchase) {
        Timber.d("Purchase complete: %s", purchase.sku)
        isLoading(true)
        activityContext.lifecycleScope.launch {
            val result: JSONObject
            try {
                result = secureUtils.makeJsonRequest(
                    secureUtils.purchaseVerifierPath, mapOf(
                        "packageName" to activityContext.packageName,
                        "productId" to purchase.sku,
                        "token" to purchase.token
                    )
                )
            } catch (e: Exception) {
                Timber.e(e)
                if (!lessVerbose) {
                    Toast.makeText(
                        activityContext,
                        R.string.purchaseVerificationFailed,
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            } finally {
                isLoading(false)
            }
            Timber.d("Verification done: %s", result.toString())
            if (secureUtils.isPurchaseValid(purchase, result)) {
                if (!secureUtils.hasPurchasedPro()) {
                    activityContext.firebaseAnalytics.logEvent(
                        FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, null
                    )
                    Toast.makeText(
                            activityContext,
                            R.string.purchaseSuccess,
                            Toast.LENGTH_LONG
                        )
                        .show()
                }
                secureUtils.onPurchaseComplete(purchase)
                completeCallback()
            } else {
                if (!lessVerbose) {
                    Toast.makeText(
                        activityContext,
                        R.string.purchaseVerificationFailed,
                        Toast.LENGTH_LONG
                    ).show()
                }
                secureUtils.onPurchaseRevert()
            }
        }
    }

    fun onDestroy() {
        if (this::checkout.isInitialized) {
            checkout.stop()
        }
    }
}