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
import androidx.work.WorkManager
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.decompilers.BaseDecompiler
import kotlinx.android.synthetic.main.activity_decompiler.*


class DecompilerActivity : BaseActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_decompiler)
        val packageFilePath = intent.getStringExtra("packageFilePath")
        val packageInfo = packageManager.getPackageArchiveInfo(packageFilePath, 0)

        itemIcon.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager))
        itemLabel.text = packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
        itemSecondaryLabel.text = if (packageInfo.versionName != null)
            packageInfo.versionName else packageInfo.versionCode.toString()

        startProcess()
    }

    fun startProcess() {
        BaseDecompiler.start(hashMapOf(
            "shouldIgnoreLibs" to true,
            "decompiler" to "cfr",
            "name" to "",
            "label" to "",
            "inputPackageFile" to ""
        ))
    }
}