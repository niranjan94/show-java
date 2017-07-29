package com.njlabs.showjava.activites.landing

import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import com.njlabs.showjava.R
import com.njlabs.showjava.activites.BaseActivity
import com.njlabs.showjava.activites.landing.adapters.HistoryListAdapter
import com.njlabs.showjava.models.SourceInfo
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_landing.*
import timber.log.Timber
import java.io.File


class LandingActivity : BaseActivity() {

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var landingHandler: LandingHandler

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_landing)
        drawerToggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        landingHandler = LandingHandler(context)
    }

    override fun postPermissionsGrant() {
        populateHistory()
    }

    private fun populateHistory() {
        landingHandler.loadHistory()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(object: Observer<ArrayList<SourceInfo>>{
                    override fun onNext(historyItems: ArrayList<SourceInfo>) {
                        SetupList(historyItems)
                    }
                    override fun onComplete() {

                    }
                    override fun onSubscribe(d: Disposable) {

                    }
                    override fun onError(e: Throwable) {
                        Timber.e(e)
                    }
                })
    }

    fun SetupList(historyItems: List<SourceInfo>) {
        if (historyItems.isEmpty()) {
            historyListView.visibility = View.GONE
            welcomeLayout.visibility = View.VISIBLE
        } else {
            welcomeLayout.visibility = View.GONE
            historyListView.visibility = View.VISIBLE
            historyListView.setHasFixedSize(true)

            val mLayoutManager = LinearLayoutManager(context)
            historyListView.layoutManager = mLayoutManager

            val historyListAdapter = HistoryListAdapter(historyItems) {
                val sourceDir = File("${Environment.getExternalStorageDirectory()}/ShowJava/sources/${it.packageName}")
                Timber.d(sourceDir.absolutePath)
            }
            historyListView.adapter = historyListAdapter
        }
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
}
