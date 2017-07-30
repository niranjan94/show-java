package com.njlabs.showjava.activities.landing

import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.landing.adapters.HistoryListAdapter
import com.njlabs.showjava.models.SourceInfo
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_landing.*
import timber.log.Timber
import java.io.File
import android.content.Intent
import com.njlabs.showjava.activities.filepicker.FilePickerActivity
import android.app.Activity
import com.njlabs.showjava.Constants
import com.njlabs.showjava.activities.apps.AppsActivity
import com.nononsenseapps.filepicker.Utils


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
        setupFab()
    }

    private fun setupFab() {
        selectionFab.setMenuListener(object : SimpleMenuListenerAdapter() {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_pick_installed -> {
                        startActivity(
                                Intent(context, AppsActivity::class.java)
                        )
                        return true
                    }
                    R.id.action_pick_sdcard -> {
                        pickFile()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun pickFile() {
        val i = Intent(context, FilePickerActivity::class.java)
        startActivityForResult(i, Constants.FILE_PICKER_RESULT)
    }

    override fun postPermissionsGrant() {
        populateHistory()
    }

    private fun populateHistory() {
        landingHandler.loadHistory()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<ArrayList<SourceInfo>> {
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

    private fun setListVisibility(isListVisible: Boolean = true) {
        val listGroupVisibility = if (isListVisible) View.VISIBLE else View.GONE
        val defaultGroupVisibility = if (isListVisible) View.GONE else View.VISIBLE
        historyListView.visibility = listGroupVisibility
        pickAppText.visibility = listGroupVisibility
        welcomeLayout.visibility = defaultGroupVisibility
    }

    fun SetupList(historyItems: List<SourceInfo>) {
        if (historyItems.isEmpty()) {
            setListVisibility(false)
        } else {
            setListVisibility(true)
            historyListView.setHasFixedSize(true)
            historyListView.layoutManager = LinearLayoutManager(context)
            val historyListAdapter = HistoryListAdapter(historyItems) { selectedHistoryItem ->
                val sourceDir = File("${Environment.getExternalStorageDirectory()}/ShowJava/sources/${selectedHistoryItem.packageName}")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.FILE_PICKER_RESULT && resultCode == Activity.RESULT_OK) {
            data?.let {
                Utils.getSelectedFilesFromResult(data)
                        .map { Utils.getFileForUri(it) }
                        .forEach { Timber.d(it.absolutePath) }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}
