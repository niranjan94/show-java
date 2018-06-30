package com.njlabs.showjava.activities.apps

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.apps.adapters.AppsListAdapter
import com.njlabs.showjava.models.PackageInfo
import com.njlabs.showjava.utils.PackageSourceTools
import com.njlabs.showjava.utils.rx.ProcessStatus
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_apps.*
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException


class AppsActivity : BaseActivity() {

    private lateinit var appsHandler: AppsHandler

    private var apps = ArrayList<PackageInfo>()

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_apps)
        appsHandler = AppsHandler(context)
        loadingView.visibility = View.VISIBLE
        appsList.visibility = View.GONE
        if (savedInstanceState != null) {
            val apps = savedInstanceState.getParcelableArrayList<PackageInfo>("apps")
            if (apps != null) {
                this.apps = apps
                setupList()
            }
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

                override fun onComplete() {

                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    Timber.e(e)
                }
            })
    }

    private fun setupList() {
        loadingView.visibility = View.GONE
        appsList.visibility = View.VISIBLE
        appsList.setHasFixedSize(true)
        appsList.layoutManager = LinearLayoutManager(context)
        val historyListAdapter = AppsListAdapter(apps) { selectedApp ->
            Timber.d(selectedApp.packageName)
            if (selectedApp.packageName.toLowerCase().contains(getString(R.string.originalApplicationId).toLowerCase())) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.checkoutSourceLink),
                    Toast.LENGTH_SHORT
                ).show()
            }
            val sourceDir = PackageSourceTools.sourceDir(selectedApp.packageName)
            Timber.d(sourceDir.absolutePath)
            if (PackageSourceTools.sourceExists(sourceDir)) {
                showAlreadyExistsDialog(selectedApp, sourceDir)
            } else {
                showDecompilerSelection(selectedApp)
            }
        }
        appsList.adapter = historyListAdapter
    }

    private fun showAlreadyExistsDialog(app: PackageInfo, sourceDir: File) {
        val alertDialog = AlertDialog.Builder(context, R.style.AlertDialog)
        alertDialog.setTitle(getString(R.string.appAlreadyDecompiled))
        alertDialog.setMessage(getString(R.string.actionAppAlreadyDecompiled))
        alertDialog.setPositiveButton(getString(R.string.viewSource)) { _, _ ->
            val i = Intent(applicationContext, AppsActivity::class.java)
            i.putExtra("java_source_dir", sourceDir.toString() + "/")
            i.putExtra("package_id", app.packageName)
            startActivity(i)
        }

        alertDialog.setNegativeButton(getString(R.string.decompile)) { _, _ ->
            try {
                FileUtils.deleteDirectory(sourceDir)
            } catch (e: IOException) {
                Timber.e(e)
            }
            showDecompilerSelection(app)
        }
        alertDialog.show()
    }

    private fun showDecompilerSelection(app: PackageInfo) {
        if (!prefs.getBoolean("hide_decompiler_select", false)) {
            val decompilerLabels = resources.getTextArray(R.array.decompilers)
            val decompilerValues = resources.getTextArray(R.array.decompilersValues)
            val builder = AlertDialog.Builder(this, R.style.AlertDialog)
            builder.setTitle(getString(R.string.pickDecompiler))
            builder.setItems(decompilerLabels) { _, item ->
                openProcessActivity(app, decompilerValues[item].toString())
            }
            val alert = builder.create()
            alert.show()
        } else {
            openProcessActivity(app, prefs.getString("decompiler", "cfr"))
        }
    }

    private fun openProcessActivity(holder: PackageInfo, decompiler: String) {
        Timber.d("FilePath:%s  Decompiler:%s", holder.packageFilePath, decompiler)
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelableArrayList("apps", apps)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app, menu)
        return true
    }


}