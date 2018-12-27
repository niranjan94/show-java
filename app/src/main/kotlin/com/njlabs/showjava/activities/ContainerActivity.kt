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

package com.njlabs.showjava.activities

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.ads.consent.ConsentStatus
import com.google.android.gms.ads.AdView
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.settings.SettingsActivity
import com.njlabs.showjava.fragments.landing.LandingFragment
import com.njlabs.showjava.fragments.landing.LandingViewModel
import com.njlabs.showjava.utils.Ads
import com.njlabs.showjava.utils.secure.PurchaseUtils
import kotlinx.android.synthetic.main.activity_container.*

class ContainerActivity: BaseActivity() {

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var purchaseUtils: PurchaseUtils


    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_container)
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.drawerOpen,
            R.string.drawerClose
        )
        navigationView.setNavigationItemSelectedListener {
            onOptionsItemSelected(it)
        }

        if (!isPro()) {
            navigationView.menu.findItem(R.id.get_pro_option).isVisible = true
        }

        drawerLayout.addDrawerListener(drawerToggle)

        purchaseUtils = PurchaseUtils(this, secureUtils)
        purchaseUtils.doOnComplete {
            if (isPro()) {
                supportActionBar?.title = "${getString(R.string.appName)} Pro"
                findViewById<AdView>(R.id.adView)?.visibility = View.GONE
                navigationView.menu.findItem(R.id.get_pro_option)?.isVisible = false
            }
        }
        purchaseUtils.initializeCheckout(false, true)
        if (inEea && userPreferences.consentStatus == ConsentStatus.UNKNOWN.ordinal) {
            Ads(context).loadConsentScreen()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (isPro()) {
            supportActionBar?.title = "${getString(R.string.appName)} Pro"
            findViewById<AdView>(R.id.adView)?.visibility = View.GONE
            navigationView.menu.findItem(R.id.get_pro_option)?.isVisible = false
        }
    }

    override fun postPermissionsGrant() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentHolder, LandingFragment())
            .commit()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        purchaseUtils.onDestroy()
    }
}