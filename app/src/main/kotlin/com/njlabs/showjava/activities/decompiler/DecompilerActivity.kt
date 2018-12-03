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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.decompilers.BaseDecompiler
import com.njlabs.showjava.decompilers.BaseDecompiler.Companion.isAvailable
import com.njlabs.showjava.utils.sourceDir
import kotlinx.android.synthetic.main.activity_decompiler.*
import kotlinx.android.synthetic.main.layout_pick_decompiler_list_item.view.*
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URI


class DecompilerActivity : BaseActivity() {

    private lateinit var packageInfo: PackageInfo

    @SuppressLint("SetTextI18n")
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_decompiler)

        loadPackageInfoFromIntent()

        if (!::packageInfo.isInitialized) {
            Toast.makeText(context, R.string.cannotDecompileFile, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val apkSize = FileUtils.byteCountToDisplaySize(packageInfo.file.length())
        itemIcon.setImageDrawable(packageInfo.loadIcon(context))
        itemLabel.text = packageInfo.label
        itemSecondaryLabel.text = "${packageInfo.version} - $apkSize"

        val decompilersValues = resources.getStringArray(R.array.decompilersValues)
        val decompilers = resources.getStringArray(R.array.decompilers)
        val decompilerDescriptions = resources.getStringArray(R.array.decompilerDescriptions)

        decompilersValues.forEachIndexed { index, decompiler ->
            if (!isAvailable(decompiler)) {
                return
            }
            val view = LayoutInflater.from(pickerList.context)
                .inflate(R.layout.layout_pick_decompiler_list_item, pickerList, false)
            view.decompilerName.text = decompilers[index]
            view.decompilerDescription.text = decompilerDescriptions[index]
            view.decompilerItemCard.cardElevation = 1F
            view.decompilerItemCard.setOnClickListener {
                startProcess(decompiler)
            }
            pickerList.addView(view)
        }

        assertSourceExistence(true)
    }

    private fun loadPackageInfoFromIntent() {
        if (intent.dataString.isNullOrEmpty()) {
            packageInfo = intent.getParcelableExtra("packageInfo")
        } else {
            val info = PackageInfo.fromFile(
                context,
                File(URI.create(intent.dataString)).canonicalFile
            )
            if (info != null) {
                packageInfo = info
            }
        }
    }

    override fun onResume() {
        super.onResume()
        assertSourceExistence()
    }

    private fun assertSourceExistence(addListener: Boolean = false) {
        val sourceDirectory = sourceDir(packageInfo.name)
        if (addListener) {
            historyCard.setOnClickListener {
                val intent = Intent(context, NavigatorActivity::class.java)
                intent.putExtra("selectedApp", SourceInfo.from(sourceDirectory))
                startActivity(intent)
            }
        }
        if (SourceInfo.exists(sourceDirectory)) {
            historyCard.visibility = View.VISIBLE
            historyInfo.text = FileUtils.byteCountToDisplaySize(
                FileUtils.sizeOfDirectory(sourceDirectory)
            )
        } else {
            historyCard.visibility = View.GONE
        }
    }

    private fun startProcess(decompiler: String) {
        BaseDecompiler.start(
            hashMapOf(
                "shouldIgnoreLibs" to userPreferences.getBoolean("ignoreLibraries", true),
                "chunkSize" to (userPreferences.getString("chunkSize", "2000")?.toInt() ?: 2000),
                "decompiler" to decompiler,
                "name" to packageInfo.name,
                "label" to packageInfo.label,
                "inputPackageFile" to packageInfo.filePath,
                "type" to packageInfo.type.ordinal
            )
        )
    }
}