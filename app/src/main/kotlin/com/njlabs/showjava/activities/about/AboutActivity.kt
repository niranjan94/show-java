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

package com.njlabs.showjava.activities.about

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_about.*

/**
 * Show information about the app, its version & licenses to all open source libraries used
 */
class AboutActivity : BaseActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_about)
        if (BuildConfig.GIT_SHA.isNotEmpty()) {
            version.setText(R.string.appVersionExtendedWithHash)
        }

        appInstanceId.text = getString(R.string.instanceId, mainApplication.instanceId)

        viewOpenSourceLicenses.setOnClickListener {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }
}