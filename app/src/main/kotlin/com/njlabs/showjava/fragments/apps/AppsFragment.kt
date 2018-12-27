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

package com.njlabs.showjava.fragments.apps

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.njlabs.showjava.BuildConfig
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.fragments.BaseFragment
import com.njlabs.showjava.fragments.apps.adapters.AppsListAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_apps.*
import timber.log.Timber


class AppsFragment : BaseFragment<AppsViewModel>() {
    override val viewModelClass = AppsViewModel::class.java
    override val layoutResource = R.layout.fragment_apps

    private lateinit var historyListAdapter: AppsListAdapter

    private var searchMenuItem: MenuItem? = null

    private var apps = ArrayList<PackageInfo>()
    private var filteredApps = ArrayList<PackageInfo>()
    private var withSystemApps: Boolean = false

    override fun init(savedInstanceState: Bundle?) {
        withSystemApps = containerActivity.userPreferences.showSystemApps
        searchMenuItem = menu?.findItem(R.id.search)

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
            filterApps(checkedId)
        }
    }

    private fun loadApps() {
        disposables.add(viewModel.loadApps(withSystemApps)
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
                            filteredApps = processStatus.result
                        }
                        setupList()
                        filterApps(R.id.userRadioButton)
                    }
                },
                { e ->
                    Timber.e(e)
                }
            )
        )
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
            Timber.d(selectedApp.name)
            if (selectedApp.name.toLowerCase().contains(BuildConfig.APPLICATION_ID.toLowerCase())) {
                Toast.makeText(context, getString(R.string.checkoutSourceLink), Toast.LENGTH_SHORT).show()
            }
            openProcessActivity(selectedApp, view)
        }
        appsList.adapter = historyListAdapter
    }

    private fun openProcessActivity(packageInfo: PackageInfo, view: View) {
        val i = Intent(context, DecompilerActivity::class.java)
        i.putExtra("packageInfo", packageInfo)
        startActivity(i)
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelableArrayList("apps", apps)
    }

    private fun searchApps(query: String?) {
        val cleanedQuery = query?.trim()?.toLowerCase() ?: ""
        historyListAdapter.updateList(filteredApps.filter {
            cleanedQuery == "" || it.label.toLowerCase().contains(cleanedQuery)
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

    override fun onDestroy() {
        super.onDestroy()
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