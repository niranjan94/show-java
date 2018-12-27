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

package com.njlabs.showjava.fragments.decompiler

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.State
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.decompiler.LowMemoryActivity
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.utils.ktx.sourceDir
import com.njlabs.showjava.workers.DecompilerWorker
import kotlinx.android.synthetic.main.fragment_decompiler_process.*
import timber.log.Timber

class DecompilerProcessFragment: BaseFragment<ViewModel>() {
    override val layoutResource = R.layout.fragment_decompiler_process

    private val statusesMap = mutableMapOf(
        "jar-extraction" to State.ENQUEUED,
        "java-extraction" to State.ENQUEUED,
        "resources-extraction" to State.ENQUEUED
    )

    private lateinit var packageInfo: PackageInfo
    private var hasCompleted = false
    private var showMemoryUsage = false
    private var ranOutOfMemory = false

    override fun init(savedInstanceState: Bundle?) {
        packageInfo = arguments!!.getParcelable("packageInfo") as PackageInfo
        showMemoryUsage = userPreferences.showMemoryUsage

        memoryUsage.visibility = if (showMemoryUsage) View.VISIBLE else View.GONE
        memoryStatus.visibility = if (showMemoryUsage) View.VISIBLE else View.GONE

        val decompilerIndex = arguments!!.getInt("decompilerIndex", 0)

        inputPackageLabel.text = packageInfo.label

        val decompilers = resources.getStringArray(R.array.decompilers)
        val decompilerValues = resources.getStringArray(R.array.decompilersValues)
        val decompilerDescriptions = resources.getStringArray(R.array.decompilerDescriptions)

        decompilerItemCard.findViewById<TextView>(R.id.decompilerName).text = decompilers[decompilerIndex]
        decompilerItemCard.findViewById<TextView>(R.id.decompilerDescription).text = decompilerDescriptions[decompilerIndex]

        setupGears()

        cancelButton.setOnClickListener {
            DecompilerWorker.cancel(context!!, packageInfo.name)
            finish()
        }

        WorkManager.getInstance()
            .getStatusesForUniqueWorkLiveData(packageInfo.name)
            .observe(this, Observer<List<WorkStatus>> { statuses ->
                statuses.forEach {
                    statusesMap.keys.forEach { tag ->
                        if (it.tags.contains(tag)) {
                            statusesMap[tag] = it.state
                        }
                    }

                    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        it.outputData.keyValueMap.forEach { t, u ->
                            Timber.d("[status][data] $t : $u")
                        }
                        statusesMap.forEach { t, u ->
                            Timber.d("[status][statuses] $t : $u")
                        }
                    }

                    if (it.outputData.getBoolean("ranOutOfMemory", false)) {
                        val intent = Intent(context, LowMemoryActivity::class.java)
                        intent.putExtra("packageInfo", packageInfo)
                        intent.putExtra("decompiler", decompilerValues[decompilerIndex])
                        startActivity(intent)
                        hasCompleted = true
                        finish()
                    } else {
                        reconcileDecompilerStatus()
                    }
                }
            })
    }

    private fun reconcileDecompilerStatus() {
        synchronized(hasCompleted) {
            if (hasCompleted) {
                return
            }

            val hasFailed = statusesMap.values.any { it == State.FAILED }
            val isWaiting = statusesMap.values.any { it == State.ENQUEUED }
            val hasPassed = statusesMap.values.all { it == State.SUCCEEDED }
            val isCancelled = statusesMap.values.any { it == State.CANCELLED }

            if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                statusesMap.forEach { t, u ->
                    Timber.d("[status] Worker: $t State: ${u.name}")
                }
            }

            Timber.d("[status] [${packageInfo.name}] hasPassed: $hasPassed | hasFailed: $hasFailed")

            when {
                isCancelled -> {
                    hasCompleted = true
                    finish()
                }
                hasFailed -> {
                    Toast.makeText(
                        context,
                        getString(R.string.errorDecompilingApp, packageInfo.label),
                        Toast.LENGTH_LONG
                    ).show()
                    hasCompleted = true
                    finish()
                }
                hasPassed -> {
                    val intent = Intent(context, NavigatorActivity::class.java)
                    intent.putExtra("selectedApp", SourceInfo.from(
                        sourceDir(
                            packageInfo.name
                        )
                    ))
                    startActivity(intent)
                    hasCompleted = true
                    finish()
                }
                isWaiting -> statusText.text = getString(R.string.waitingToStart)
            }
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
        leftProgressGear.animation = getGearAnimation(2, true)
        rightProgressGear.animation = getGearAnimation(1, false)
    }

    private val progressReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n") // For memory status
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra(Constants.WORKER.STATUS_MESSAGE)
            if (intent.getStringExtra(Constants.WORKER.STATUS_TYPE) == "memory") {
                if (!showMemoryUsage) {
                    return
                }
                try {
                    val percentage = message.toDouble()
                    memoryStatus.text = "$message%"
                    val textColor = ContextCompat.getColor(
                        context,
                        when {
                            percentage < 40 -> R.color.green_500
                            percentage < 60 -> R.color.amber_500
                            percentage < 80 -> R.color.orange_500
                            else -> R.color.red_500
                        }
                    )
                    memoryStatus.setTextColor(textColor)
                    memoryUsage.setTextColor(textColor)
                } catch (ignored: Exception) { }
                return
            }

            intent.getStringExtra(Constants.WORKER.STATUS_TITLE)?.let {
                if (it.trim().isNotEmpty()) {
                    statusTitle.text = it
                }
            }
            message?.let {
                statusText.text = it
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val statusIntentFilter = IntentFilter(Constants.WORKER.ACTION.BROADCAST + packageInfo.name)
        containerActivity.registerReceiver(progressReceiver, statusIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        try {
            containerActivity.unregisterReceiver(progressReceiver)
        } catch (ignored: Exception) { }
    }
}