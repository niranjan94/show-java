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
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.apps.adapters.AppsListAdapter
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.utils.PackageSourceTools
import com.njlabs.showjava.utils.rx.ProcessStatus
import io.reactivex.Observer
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
        appsHandler.loadApps()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<ProcessStatus<ArrayList<PackageInfo>>> {
                override fun onNext(processStatus: ProcessStatus<ArrayList<PackageInfo>>) {
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
                }
                override fun onComplete() {}
                override fun onSubscribe(d: Disposable) {}
                override fun onError(e: Throwable) { Timber.e(e) }
            })
    }

    private fun setupList() {
        loadingView.visibility = View.GONE
        appsList.visibility = View.VISIBLE
        searchMenuItem?.isVisible = true
        appsList.setHasFixedSize(true)
        appsList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        historyListAdapter = AppsListAdapter(apps) { selectedApp: PackageInfo, view: View ->
            Timber.d(selectedApp.name)
            if (selectedApp.name.toLowerCase().contains(getString(R.string.originalApplicationId).toLowerCase())) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.checkoutSourceLink),
                    Toast.LENGTH_SHORT
                ).show()
            }
            val sourceDir = PackageSourceTools.sourceDir(selectedApp.name)
            Timber.d(sourceDir.canonicalPath)
            openProcessActivity(selectedApp, view)
        }
        appsList.adapter = historyListAdapter
    }

    private fun openProcessActivity(packageInfo: PackageInfo, view: View) {
        val i = Intent(applicationContext, DecompilerActivity::class.java)
        Timber.d("packageFilePath:%s", packageInfo.filePath)
        i.putExtra("packageFilePath", packageInfo.filePath)

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

}