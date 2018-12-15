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
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.utils.secure.PurchaseUtils
import kotlinx.android.synthetic.main.activity_purchase.*
import org.solovyev.android.checkout.*


class PurchaseActivity : BaseActivity() {

    private lateinit var purchaseUtils: PurchaseUtils

    private fun isLoading(loading: Boolean) {
        buttonProgressBar.visibility = if (!loading) View.GONE else View.VISIBLE
        buyButton.visibility = if (loading) View.GONE else View.VISIBLE
    }

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_purchase, getString(R.string.appNameGetPro))

        secureUtils.isSafeExtended(
            {
                runOnUiThread {
                    isLoading(false)
                    purchaseUtils = PurchaseUtils(this, secureUtils) {
                        isLoading(it)
                    }
                    purchaseUtils.initializeCheckout(true)
                    buyButton.setOnClickListener {
                        isLoading(true)
                        makePurchase()
                    }
                }
            },
            {
                runOnUiThread {
                    isLoading(false)
                    buyButton.visibility = View.GONE
                }
            },
            {
                runOnUiThread {
                    isLoading(false)
                    buyButton.visibility = View.GONE
                }
            }
        )
    }

    private fun makePurchase() {
        purchaseUtils.checkout.whenReady(object : Checkout.EmptyListener() {
            override fun onReady(requests: BillingRequests) {
                requests.purchase(
                    ProductTypes.IN_APP,
                    secureUtils.iapProductId,
                    null,
                    purchaseUtils.checkout.purchaseFlow
                )
            }
        })
    }

    override fun onDestroy() {
        purchaseUtils.onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        purchaseUtils.checkout.onActivityResult(requestCode, resultCode, data)
    }
}