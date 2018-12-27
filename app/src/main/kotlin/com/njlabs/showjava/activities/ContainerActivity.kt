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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.ads.consent.ConsentStatus
import com.google.android.gms.ads.AdView
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.about.AboutActivity
import com.njlabs.showjava.activities.purchase.PurchaseActivity
import com.njlabs.showjava.fragments.landing.LandingFragment
import com.njlabs.showjava.fragments.settings.SettingsFragment
import com.njlabs.showjava.utils.Ads
import com.njlabs.showjava.utils.secure.PurchaseUtils
import kotlinx.android.synthetic.main.activity_container.*


class ContainerActivity: BaseActivity() {

    private lateinit var purchaseUtils: PurchaseUtils

    private var homeShouldOpenDrawer = true


    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_container)
        navigationView.setNavigationItemSelectedListener {
            onOptionsItemSelected(it)
        }

        if (!isPro()) {
            navigationView.menu.findItem(R.id.get_pro_option).isVisible = true
        }

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

        enableDrawerIcon(true)

        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.findFragmentById(R.id.fragmentHolder)?.let {
                if (it is LandingFragment) {
                    enableDrawerIcon(true)
                } else {
                    enableDrawerIcon(false)
                }
            }
        }
    }

    private fun enableDrawerIcon(enable: Boolean) {
        homeShouldOpenDrawer = if (enable) {
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_menu_white)
            true
        } else {
            supportActionBar!!.setHomeAsUpIndicator(null)
            false
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
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentHolder, LandingFragment())
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        purchaseUtils.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawers()

        when (item.itemId) {
            android.R.id.home -> {
                if (homeShouldOpenDrawer) {
                    drawerLayout.openDrawer(GravityCompat.START)
                } else {
                    onBackPressed()
                }
                return true
            }
            R.id.about_option -> {
                startActivity(Intent(baseContext, AboutActivity::class.java))
                return true
            }
            R.id.settings_option -> {
                gotoFragment(SettingsFragment())
                return true
            }
            R.id.get_pro_option -> {
                startActivity(Intent(baseContext, PurchaseActivity::class.java))
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun gotoFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentHolder, fragment)
            .addToBackStack(null)
            .commit()
    }
}