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

package com.njlabs.showjava.fragments.landing

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.fragments.apps.AppsFragment
import com.njlabs.showjava.fragments.decompiler.DecompilerFragment
import com.njlabs.showjava.fragments.explorer.navigator.NavigatorFragment
import com.njlabs.showjava.fragments.landing.adapters.HistoryListAdapter
import com.njlabs.showjava.utils.ktx.bundleOf
import kotlinx.android.synthetic.main.fragment_landing.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class LandingFragment : BaseFragment<LandingViewModel>() {

    override val layoutResource: Int = R.layout.fragment_landing

    private lateinit var filePickerDialog: FilePickerDialog
    override val viewModel by viewModels<LandingViewModel>()

    private var historyListAdapter: HistoryListAdapter? = null
    private var historyItems = ArrayList<SourceInfo>()

    private var shouldLoadHistory = true

    override fun init(savedInstanceState: Bundle?) {

        if (savedInstanceState != null) {
            savedInstanceState.getParcelableArrayList<SourceInfo>("historyItems")?.let {
                this.historyItems = it
                shouldLoadHistory = false
                setupList()
            }
        }

        setupFab()

        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = Environment.getExternalStorageDirectory()
        properties.error_dir = properties.root
        properties.offset = properties.root
        properties.extensions = arrayOf("apk", "jar", "dex", "odex")

        filePickerDialog = FilePickerDialog(context, properties)
        filePickerDialog.setTitle(getString(R.string.selectFile))

        filePickerDialog.setDialogSelectionListener { files ->
            if (files.isNotEmpty()) {
                val selectedFile = File(files.first())
                if (selectedFile.exists() && selectedFile.isFile) {
                    PackageInfo.fromFile(requireContext(), selectedFile)?.let {
                        containerActivity.gotoFragment(
                            DecompilerFragment(), bundleOf(
                                "packageInfo" to it
                            )
                        )
                    }
                }
            }
        }

        populateHistory()

        swipeRefresh.setOnRefreshListener {
            populateHistory(true)
        }

    }

    override fun onResume() {
        super.onResume()
        populateHistory(true)
    }

    private fun setupFab() {
        selectionFab.addOnMenuItemClickListener { _, _, itemId ->
            when (itemId) {
                R.id.action_pick_installed -> {
                    containerActivity.gotoFragment(AppsFragment())
                }
                R.id.action_pick_sdcard -> {
                    pickFile()
                }
            }
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val mimetypes = arrayOf(
            "application/vnd.android.package-archive",
            "application/java-archive",
            "application/x-dex"
        )
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
        startActivityForResult(intent, Constants.FILE_PICKER_REQUEST)
    }

    private fun populateHistory(resume: Boolean = false) {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            historyItems = try {
                viewModel.loadHistory()
            } catch (e: Exception) {
                Timber.e(e)
                ArrayList()
            }
            swipeRefresh.isRefreshing = false
            if (resume && historyListAdapter != null) {
                historyListAdapter?.updateData(historyItems)
                setListVisibility(historyItems.isNotEmpty())
            } else {
                setupList()
            }
        }
    }

    private fun setListVisibility(isListVisible: Boolean = true) {
        val listGroupVisibility = if (isListVisible) View.VISIBLE else View.GONE
        val defaultGroupVisibility = if (isListVisible) View.GONE else View.VISIBLE
        historyListView.visibility = listGroupVisibility
        swipeRefresh.visibility = listGroupVisibility
        pickAppText.visibility = listGroupVisibility
        welcomeLayout.visibility = defaultGroupVisibility
    }

    private fun setupList() {
        if (historyItems.isEmpty()) {
            setListVisibility(false)
        } else {
            setListVisibility(true)
            historyListView.setHasFixedSize(true)
            historyListView.layoutManager = LinearLayoutManager(context)
            historyListAdapter = HistoryListAdapter(historyItems) { selectedHistoryItem ->
                containerActivity.gotoFragment(NavigatorFragment(), bundleOf(
                    "selectedApp" to selectedHistoryItem
                ))
            }
            historyListView.adapter = historyListAdapter
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult resultCode=${Constants.FILE_PICKER_REQUEST}")
        if (resultCode == Constants.FILE_PICKER_REQUEST) {
            Timber.d("onActivityResult data.dataString=${data?.dataString}")
            Timber.d("onActivityResult data.data=${data?.data}")
            Timber.d("onActivityResult data=${data}")
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("historyItems", historyItems)
    }
}