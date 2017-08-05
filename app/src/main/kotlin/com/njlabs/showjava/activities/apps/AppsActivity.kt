package com.njlabs.showjava.activities.apps

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.apps.adapters.AppsListAdapter
import com.njlabs.showjava.models.PackageInfo
import com.njlabs.showjava.utils.rx.ProcessStatus
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_apps.*
import timber.log.Timber

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
                                secondaryStatusText.text = processStatus.secondaryStatus
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
        apps.let {
            appsList.visibility = View.VISIBLE
            appsList.setHasFixedSize(true)
            appsList.layoutManager = LinearLayoutManager(context)
            val historyListAdapter = AppsListAdapter(apps) { selectedApp ->
                Timber.d(selectedApp.packageName)
            }
            appsList.adapter = historyListAdapter
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        apps.let {
            bundle.putParcelableArrayList("apps", apps)
        }
    }

}