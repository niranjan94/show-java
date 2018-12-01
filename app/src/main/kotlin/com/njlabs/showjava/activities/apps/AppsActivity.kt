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

package com.njlabs.showjava.activities.apps

import android.app.ActivityOptions
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.apps.adapters.AppsListAdapter
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.data.PackageInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_apps.*
import timber.log.Timber


class AppsActivity : BaseActivity(), SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private lateinit var appsHandler: AppsHandler
    private lateinit var historyListAdapter: AppsListAdapter

    private var searchMenuItem: MenuItem? = null

    private var apps = ArrayList<PackageInfo>()
    private var appsLoaderSubscription: Disposable? = null

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_apps)
        appsHandler = AppsHandler(context)
        if (savedInstanceState != null) {
            val apps = savedInstanceState.getParcelableArrayList<PackageInfo>("apps")
            if (apps != null) {
                this.apps = apps
                setupList()
            }
        } else {
            loadingView.visibility = View.VISIBLE
            appsList.visibility = View.GONE
            searchMenuItem?.isVisible = false
        }
        if (this.apps.isEmpty()) {
            loadApps()
        }
    }

    private fun loadApps() {
        appsLoaderSubscription = appsHandler.loadApps()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { processStatus ->
                    if (!processStatus.isDone) {
                        progressBar.progress = processStatus.progress.toInt()
                        statusText.text = processStatus.status
                        processStatus.secondaryStatus?.let {
                            secondaryStatusText.text = it
                        }
                    } else {
                        if (processStatus.result != null) {
                            apps = processStatus.result
                        }
                        setupList()
                    }
                },
                { e ->
                    Timber.e(e)
                }
            )
    }

    private fun setupList() {
        loadingView.visibility = View.GONE
        appsList.visibility = View.VISIBLE
        searchMenuItem?.isVisible = true
        appsList.setHasFixedSize(true)
        appsList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        historyListAdapter = AppsListAdapter(apps) { selectedApp: PackageInfo, view: View ->
            Timber.d(selectedApp.name)
            if (selectedApp.name.toLowerCase().contains(BuildConfig.APPLICATION_ID.toLowerCase())) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.checkoutSourceLink),
                    Toast.LENGTH_SHORT
                ).show()
            }
            openProcessActivity(selectedApp, view)
        }
        appsList.adapter = historyListAdapter
    }

    private fun openProcessActivity(packageInfo: PackageInfo, view: View) {
        val i = Intent(applicationContext, DecompilerActivity::class.java)
        i.putExtra("packageInfo", packageInfo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val options = ActivityOptions
                .makeSceneTransitionAnimation(this, view.findViewById(R.id.itemCard), "appListItem")
            return startActivity(i, options.toBundle())
        }

        startActivity(i)
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelableArrayList("apps", apps)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        val searchView = searchMenuItem?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(this)
        searchView.setOnCloseListener(this)
        return true
    }

    fun searchApps(query: String?) {
        val filteredApps = ArrayList<PackageInfo>()
        val cleanedQuery = query?.trim()?.toLowerCase() ?: ""
        for (app in apps) {
            if (cleanedQuery == "" || app.label.toLowerCase().contains(cleanedQuery)) {
                filteredApps.add(app)
            }
        }
        historyListAdapter.updateList(filteredApps)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchApps(query)
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        searchApps(query)
        return true
    }

    override fun onClose(): Boolean {
        searchApps(null)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (appsLoaderSubscription?.isDisposed != true) {
            appsLoaderSubscription?.dispose()
        }
    }
}