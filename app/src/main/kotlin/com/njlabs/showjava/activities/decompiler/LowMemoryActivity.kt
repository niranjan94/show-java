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

import android.os.Bundle
import android.widget.Toast
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.data.PackageInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_low_memory.*
import timber.log.Timber

class LowMemoryActivity : BaseActivity() {

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_low_memory)
        val packageInfo = intent.getParcelableExtra<PackageInfo>("packageInfo")
        val decompiler = intent.getStringExtra("decompiler")

        reportButton.setOnClickListener {
            disposables.add(
                secureUtils
                    .makeJsonRequest(
                        "/report-app", mapOf(
                            "label" to packageInfo.label,
                            "name" to packageInfo.name,
                            "type" to packageInfo.type.name,
                            "decompiler" to decompiler
                        )
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe (
                        { response ->
                            Timber.d("[bug-report] success: $response")
                        },
                        { error ->
                            Timber.d("[bug-report][error] $error")
                        }
                    )
            )
            Toast.makeText(context, R.string.appReportThanks, Toast.LENGTH_LONG).show()
        }
    }

}