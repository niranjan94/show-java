package com.njlabs.showjava.activites.landing

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import com.njlabs.showjava.R
import com.njlabs.showjava.activites.BaseActivity
import com.njlabs.showjava.models.SourceInfo
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

import kotlinx.android.synthetic.main.activity_landing.drawerLayout
import timber.log.Timber

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
                        Timber.d(historyItems.toString())
                        Timber.d(historyItems.size.toString())
                        for (historyItem in historyItems) {
                            Timber.d(historyItem.packageName)
                        }
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
