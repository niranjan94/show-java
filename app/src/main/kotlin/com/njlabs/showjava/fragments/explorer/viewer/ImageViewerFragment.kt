/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2019 Niranjan Rajendran
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

package com.njlabs.showjava.fragments.explorer.viewer

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.ImageViewState
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.njlabs.showjava.R
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.utils.ktx.sourceDir
import kotlinx.android.synthetic.main.fragment_image_viewer.*
import org.apache.commons.io.FilenameUtils

class ImageViewerFragment : BaseFragment<ViewModel>() {
    override val layoutResource = R.layout.fragment_image_viewer
    override val viewModel by viewModels<ViewModel>()

    override var isBlack: Boolean = true
    private val bundleState = "ImageViewState"

    private lateinit var fileName: String
    private lateinit var packageName: String
    private lateinit var subtitle: String

    override fun init(savedInstanceState: Bundle?) {
        arguments?.let {
            var imageViewState: ImageViewState? = null
            if (savedInstanceState != null && savedInstanceState.containsKey(bundleState)) {
                imageViewState = savedInstanceState.getSerializable(bundleState) as ImageViewState
                isBlack = savedInstanceState.getBoolean("isBlack", true)
            }
            val filePath = it.getString("filePath")
            this.packageName = it.getString("name")!!
            this.fileName = FilenameUtils.getName(filePath)
            this.subtitle = FilenameUtils
                .getFullPath(filePath)
                .replace(
                    sourceDir(this.packageName).canonicalPath,
                    ""
                )
            imageView.setImage(ImageSource.uri(filePath!!), imageViewState)
            imageView.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
            imageView.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER)
            imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
            imageView.setMinimumDpi(100)
            imageView.setMaximumDpi(600)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = imageView.state
        if (state != null) {
            outState.putSerializable(bundleState, imageView.state)
        }
        outState.putBoolean("isBlack", isBlack)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.invert_colors -> {
                isBlack = !isBlack
                setDecor()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setDecor(overrideIsBlack: Boolean) {
        super.setDecor(overrideIsBlack)
        if (overrideIsBlack) {
            imageView.setBackgroundColor(Color.BLACK)
        } else {
            imageView.setBackgroundColor(Color.WHITE)
        }
    }

    override fun onSetToolbar(menu: Menu) {
        super.onSetToolbar(menu)
        setDecor()
        containerActivity.supportActionBar?.title = fileName
        if (fileName.trim().equals("icon.png", true)) {
            containerActivity.setSubtitle(packageName)
        } else {
            containerActivity.setSubtitle(subtitle)
        }
        menu.findItem(R.id.invert_colors).isVisible = true
    }

    override fun onResetToolbar(menu: Menu) {
        super.onResetToolbar(menu)
        setDecor(false)
        menu.findItem(R.id.invert_colors).isVisible = false
    }
}