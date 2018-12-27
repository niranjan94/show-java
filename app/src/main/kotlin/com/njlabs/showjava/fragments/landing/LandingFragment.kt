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

package com.njlabs.showjava.fragments.landing

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import com.njlabs.showjava.fragments.landing.adapters.HistoryListAdapter
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.fragments.apps.AppsFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_landing.*
import timber.log.Timber
import java.io.File

class LandingFragment : BaseFragment<LandingViewModel>() {

    override val viewModelClass: Class<LandingViewModel> = LandingViewModel::class.java
    override val layoutResource: Int = R.layout.fragment_landing

    private lateinit var filePickerDialog: FilePickerDialog

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
                    PackageInfo.fromFile(context!!, selectedFile)?.let {
                        val i = Intent(context, DecompilerActivity::class.java)
                        i.putExtra("packageInfo", it)
                        startActivity(i)
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
        filePickerDialog.show()
    }

    private fun populateHistory(resume: Boolean = false) {
        swipeRefresh.isRefreshing = true
        disposables.add(viewModel.loadHistory()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                Timber.e(it)
                ArrayList()
            }
            .subscribe {
                historyItems = it
                swipeRefresh.isRefreshing = false
                if (resume && historyListAdapter != null) {
                    historyListAdapter?.updateData(historyItems)
                    setListVisibility(!historyItems.isEmpty())
                } else {
                    setupList()
                }
            }
        )
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
                val intent = Intent(context, NavigatorActivity::class.java)
                intent.putExtra("selectedApp", selectedHistoryItem)
                startActivity(intent)
            }
            historyListView.adapter = historyListAdapter
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelableArrayList("historyItems", historyItems)
    }
}