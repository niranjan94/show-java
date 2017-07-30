package com.njlabs.showjava.activities.apps

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
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

class AppsActivity: BaseActivity() {

    private lateinit var appsHandler: AppsHandler

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_apps)
        appsHandler = AppsHandler(context)
        loadingView.visibility = View.VISIBLE
        appsList.visibility = View.GONE
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
                            setupList(processStatus.result)
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

    private fun setupList(apps: ArrayList<PackageInfo>?) {
        loadingView.visibility = View.GONE
        if (apps != null) {
            appsList.visibility = View.VISIBLE
            appsList.setHasFixedSize(true)
            appsList.layoutManager =  LinearLayoutManager(context)
            /**appsList.addItemDecoration(DividerItemDecoration(
                    appsList.context,
                    (appsList.layoutManager as LinearLayoutManager).orientation
            ))**/
            val historyListAdapter = AppsListAdapter(apps) { selectedApp ->
                Timber.d(selectedApp.packageName)
            }
            appsList.adapter = historyListAdapter
        }
    }
}