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

package com.njlabs.showjava.activities.landing

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.google.android.gms.ads.AdView
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.apps.AppsActivity
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import com.njlabs.showjava.activities.landing.adapters.HistoryListAdapter
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.utils.secure.PurchaseUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_landing.*
import timber.log.Timber
import java.io.File


class LandingActivity : BaseActivity() {

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var landingHandler: LandingHandler
    private lateinit var filePickerDialog: FilePickerDialog
    private lateinit var purchaseUtils: PurchaseUtils

    private var historyListAdapter: HistoryListAdapter? = null
    private var historyItems = ArrayList<SourceInfo>()

    private var shouldLoadHistory = true

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_landing)
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.drawerOpen,
            R.string.drawerClose
        )
        navigationView.setNavigationItemSelectedListener {
            onOptionsItemSelected(it)
        }

        if (!isPro()) {
            navigationView.menu.findItem(R.id.get_pro_option).isVisible = true
        }

        drawerLayout.addDrawerListener(drawerToggle)
        landingHandler = LandingHandler(context)
        setupFab()

        if (savedInstanceState != null) {
            val historyItems = savedInstanceState.getParcelableArrayList<SourceInfo>("historyItems")
            if (historyItems != null) {
                this.historyItems = historyItems
                shouldLoadHistory = false
                setupList()
            }
        }

        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = Environment.getExternalStorageDirectory()
        properties.error_dir = properties.root
        properties.offset = properties.root
        properties.extensions = arrayOf("apk", "jar", "dex", "odex")

        filePickerDialog = FilePickerDialog(this, properties)
        filePickerDialog.setTitle(getString(R.string.selectFile))

        filePickerDialog.setDialogSelectionListener { files ->
            if (files.isNotEmpty()) {
                val selectedFile = File(files.first())
                if (selectedFile.exists() && selectedFile.isFile) {
                    PackageInfo.fromFile(context, selectedFile) ?. let {
                        val i = Intent(applicationContext, DecompilerActivity::class.java)
                        i.putExtra("packageInfo", it)
                        startActivity(i)
                    }
                }
            }
        }

        swipeRefresh.setOnRefreshListener {
            populateHistory(true)
        }

        purchaseUtils = PurchaseUtils(this, secureUtils)
        purchaseUtils.doOnComplete {
            if (isPro()) {
                supportActionBar?.title = "${getString(R.string.appName)} Pro"
                findViewById<AdView>(R.id.adView)?.visibility = View.GONE
                navigationView.menu.findItem(R.id.get_pro_option)?.isVisible = false
            }
        }
        purchaseUtils.initializeCheckout(false)
    }

    public override fun onResume() {
        super.onResume()
        if (hasValidPermissions()) {
            populateHistory(true)
        }
        if (isPro()) {
            supportActionBar?.title = "${getString(R.string.appName)} Pro"
            findViewById<AdView>(R.id.adView)?.visibility = View.GONE
            navigationView.menu.findItem(R.id.get_pro_option)?.isVisible = false
        }
    }

    private fun setupFab() {
        selectionFab.addOnMenuItemClickListener { _, _, itemId ->
            when (itemId) {
                R.id.action_pick_installed -> {
                    startActivity(
                        Intent(context, AppsActivity::class.java)
                    )
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

    override fun postPermissionsGrant() {
        if (shouldLoadHistory) {
            populateHistory()
        }
    }

    private fun populateHistory(resume: Boolean = false) {
        swipeRefresh.isRefreshing = true
        disposables.add(landingHandler.loadHistory()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.e(it) }
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

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        purchaseUtils.onDestroy()
    }
}
