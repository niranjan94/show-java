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

package com.njlabs.showjava.activities.explorer.viewer

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.ImageViewState
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_image_viewer.*
import org.apache.commons.io.FilenameUtils


class ImageViewerActivity : BaseActivity() {
    private var isBlack: Boolean = true
    private val bundleState = "ImageViewState"

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_image_viewer)
        window.decorView.setBackgroundColor(Color.BLACK)
        toolbar.popupTheme = R.style.AppTheme_DarkPopupOverlay

        val extras = intent.extras
        extras?.let {

            var imageViewState: ImageViewState? = null
            if (savedInstanceState != null && savedInstanceState.containsKey(bundleState)) {
                imageViewState = savedInstanceState.getSerializable(bundleState) as ImageViewState
            }

            val filePath = it.getString("filePath")
            val packageName = it.getString("name")
            val fileName = FilenameUtils.getName(filePath)
            supportActionBar?.title = fileName
            val subtitle = FilenameUtils
                .getFullPath(filePath)
                .replace(
                    "${Environment.getExternalStorageDirectory()}/show-java/sources/$packageName/",
                    ""
                )

            if (fileName.trim().equals("icon.png", true)) {
                setSubtitle(packageName)
            } else {
                setSubtitle(subtitle)
            }

            imageView.setImage(ImageSource.uri(filePath!!), imageViewState)
            imageView.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
            imageView.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER)
            imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
            imageView.setMinimumDpi(100)
            imageView.setMaximumDpi(600)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.invert_colors).isVisible = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.invert_colors -> {
                if (isBlack) {
                    window.decorView.setBackgroundColor(Color.WHITE)
                } else {
                    window.decorView.setBackgroundColor(Color.BLACK)
                }
                isBlack = !isBlack
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = imageView.state
        if (state != null) {
            outState.putSerializable(bundleState, imageView.state)
        }
    }
}