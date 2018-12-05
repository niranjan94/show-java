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

package com.njlabs.showjava.activities.decompiler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.landing.LandingActivity
import com.njlabs.showjava.data.PackageInfo
import kotlinx.android.synthetic.main.activity_decompiler_process.*
import android.content.IntentFilter
import com.njlabs.showjava.Constants
import com.njlabs.showjava.workers.DecompilerWorker


class DecompilerProcessActivity : BaseActivity() {


    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_decompiler_process)
        val packageInfo = intent.getParcelableExtra<PackageInfo>("packageInfo")
        val decompilerIndex = intent.getIntExtra("decompilerIndex", 0)
        if (packageInfo != null) {
            inputPackageLabel.text = packageInfo.label
        }

        val decompilers = resources.getStringArray(R.array.decompilers)
        val decompilerDescriptions = resources.getStringArray(R.array.decompilerDescriptions)

        decompilerItemCard.findViewById<TextView>(R.id.decompilerName).text = decompilers[decompilerIndex]
        decompilerItemCard.findViewById<TextView>(R.id.decompilerDescription).text = decompilerDescriptions[decompilerIndex]

        setupGears()

        val statusIntentFilter = IntentFilter(Constants.WORKER.ACTION.BROADCAST + packageInfo.name)
        registerReceiver(progressReceiver, statusIntentFilter)

        cancelButton.setOnClickListener {
            DecompilerWorker.cancel(context, packageInfo.name)
            onBackPressed()
        }
    }

    private fun getGearAnimation(duration: Int = 1, isClockwise: Boolean = true): RotateAnimation {
        val animation = RotateAnimation(
            if (isClockwise) 0.0f else 360.0f,
            if (isClockwise) 360.0f else 0.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        animation.repeatCount = Animation.INFINITE
        animation.duration = duration.toLong() * 1500
        animation.interpolator = LinearInterpolator()
        return animation
    }

    private fun setupGears() {
        leftProgressGear.post { leftProgressGear.animation = getGearAnimation(2, true) }
        rightProgressGear.post { rightProgressGear.animation = getGearAnimation(1, false) }
    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(Constants.WORKER.STATUS_KEY)?.let {
                if (it.trim().isNotEmpty()) {
                    statusTitle.text = it
                }
            }
            intent.getStringExtra(Constants.WORKER.STATUS_MESSAGE)?.let {
                statusText.text = it
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(progressReceiver)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this, LandingActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}