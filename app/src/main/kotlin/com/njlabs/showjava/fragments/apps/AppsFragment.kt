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

package com.njlabs.showjava.fragments.apps

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.fragments.apps.adapters.AppsListAdapter
import com.njlabs.showjava.fragments.decompiler.DecompilerFragment
import com.njlabs.showjava.utils.ktx.bundleOf
import kotlinx.android.synthetic.main.fragment_apps.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class AppsFragment : BaseFragment<AppsViewModel>() {
    override val layoutResource = R.layout.fragment_apps
    override val viewModel by viewModels<AppsViewModel>()

    private lateinit var historyListAdapter: AppsListAdapter

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    private var apps = ArrayList<PackageInfo>()
    private var filteredApps = ArrayList<PackageInfo>()
    private var withSystemApps = false

    override fun init(savedInstanceState: Bundle?) {
        withSystemApps = containerActivity.userPreferences.showSystemApps
        showList(false)

        if (savedInstanceState != null) {
            val apps = savedInstanceState.getParcelableArrayList<PackageInfo>("apps")
            if (!apps.isNullOrEmpty()) {
                this.apps = apps
                this.filteredApps = apps
                setupList()
                filterApps(R.id.userRadioButton)
            }
        } else {
            if (this.apps.isNullOrEmpty()) {
                loadApps()
            } else {
                setupList()
            }
        }

        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            containerActivity.collapseSearch()
            filterApps(checkedId)
        }
    }

    private fun loadApps() {
        lifecycleScope.launch {
            try {
                apps = viewModel.loadApps(withSystemApps) { progress, status, secondaryStatus ->
                    withContext(Dispatchers.Main) {
                        progressBar.progress = progress.toInt()
                        statusText.text = status
                        secondaryStatus.let {
                            secondaryStatusText.text = it
                        }
                    }
                }
                filteredApps = apps
                setupList()
                filterApps(R.id.userRadioButton)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun showList(isListVisible: Boolean) {
        loadingView.visibility = if (!isListVisible) View.VISIBLE else View.GONE
        appsList.visibility = if (isListVisible) View.VISIBLE else View.GONE
        typeRadioGroup.visibility = if (withSystemApps && isListVisible) View.VISIBLE else View.GONE
        searchMenuItem?.isVisible = isListVisible
    }

    private fun setupList() {
        showList(true)
        appsList.setHasFixedSize(true)
        appsList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        historyListAdapter = AppsListAdapter(apps) { selectedApp: PackageInfo, view: View ->
            containerActivity.collapseSearch()
            if (selectedApp.name.toLowerCase(Locale.ROOT).contains(BuildConfig.APPLICATION_ID.toLowerCase(Locale.ROOT))) {
                Toast.makeText(context, getString(R.string.checkoutSourceLink), Toast.LENGTH_SHORT).show()
            }
            openProcessActivity(selectedApp, view)
        }
        appsList.adapter = historyListAdapter
    }

    private fun openProcessActivity(packageInfo: PackageInfo, view: View) {
        containerActivity.gotoFragment(DecompilerFragment(), bundleOf(
            "packageInfo" to packageInfo,
            "transitionName" to getString(R.string.appsListItemTransitionName)
        ), view.findViewById(R.id.itemCard))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("apps", apps)
    }

    private fun searchApps(query: String?) {
        val cleanedQuery = query?.trim()?.toLowerCase(Locale.ROOT) ?: ""
        historyListAdapter.updateList(filteredApps.filter {
            cleanedQuery == "" || it.label.toLowerCase(Locale.ROOT).contains(cleanedQuery)
        })
    }

    private fun filterApps(filterId: Int) {
        filteredApps = apps.filter {
            when(filterId) {
                R.id.systemRadioButton -> it.isSystemPackage
                R.id.userRadioButton -> !it.isSystemPackage
                else -> true
            }
        } as ArrayList<PackageInfo>
        historyListAdapter.updateList(filteredApps)
    }

    override fun onSetToolbar(menu: Menu) {
        super.onSetToolbar(menu)
        searchMenuItem = menu.findItem(R.id.search)
        searchView = menu.findItem(R.id.search)?.actionView as SearchView?
        searchMenuItem?.isVisible = appsList.visibility == View.VISIBLE
    }

    override fun onResetToolbar(menu: Menu) {
        super.onResetToolbar(menu)
        searchMenuItem?.isVisible = false
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
}