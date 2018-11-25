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

package com.njlabs.showjava.activities.purchase

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_purchase.*
import org.solovyev.android.checkout.*
import timber.log.Timber


class PurchaseActivity : BaseActivity() {

    private lateinit var checkout: ActivityCheckout
    private lateinit var inventory: Inventory

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_purchase, getString(R.string.appNameGetPro))

        safetyNet.isSafeExtended(
            {
                runOnUiThread {
                    buttonProgressBar.visibility = View.GONE
                    buyButton.visibility = View.VISIBLE
                }
            },
            {
                runOnUiThread {
                    buttonProgressBar.visibility = View.GONE
                    buyButton.visibility = View.GONE
                }
            },
            {
                runOnUiThread {
                    buttonProgressBar.visibility = View.GONE
                    buyButton.visibility = View.GONE
                }
            }
        )

        checkout = Checkout.forActivity(this, safetyNet.getBilling())
        checkout.start()
        checkout.createPurchaseFlow(PurchaseListener())

        inventory = checkout.makeInventory()
        inventory.load(
            Inventory.Request.create()
                .loadAllPurchases()
                .loadSkus(ProductTypes.IN_APP, safetyNet.getIapProductId()),
            InventoryCallback()
        )

        buyButton.setOnClickListener {
            makePurchase()
        }
    }

    private fun onPurchaseComplete(purchase: Purchase) {
        Timber.d("Purchase complete: %s", purchase.sku)
    }

    private fun makePurchase() {
        checkout.whenReady(object : Checkout.EmptyListener() {
            override fun onReady(requests: BillingRequests) {
                requests.purchase(
                    ProductTypes.IN_APP,
                    safetyNet.getIapProductId(),
                    null,
                    checkout.purchaseFlow
                )
            }
        })
    }

    private inner class PurchaseListener : EmptyRequestListener<Purchase>() {
        override fun onSuccess(purchase: Purchase) {
            if (purchase.sku == safetyNet.getIapProductId() && purchase.state == Purchase.State.PURCHASED) {
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
            Toast.makeText(context, getString(messageKey), Toast.LENGTH_SHORT).show()
        }
    }

    private inner class InventoryCallback : Inventory.Callback {
        override fun onLoaded(products: Inventory.Products) {
            products.forEach {
                val isPurchased = it.isPurchased(safetyNet.getIapProductId())
                if (isPurchased) {
                    val purchase =
                        it.getPurchaseInState(safetyNet.getIapProductId(), Purchase.State.PURCHASED)
                    if (purchase != null) {
                        onPurchaseComplete(purchase)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (this::checkout.isInitialized) {
            checkout.stop()
        }
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        checkout.onActivityResult(requestCode, resultCode, data)
    }
}