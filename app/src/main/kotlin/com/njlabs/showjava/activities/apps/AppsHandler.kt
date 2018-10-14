package com.njlabs.showjava.activities.apps

import android.content.Context
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.utils.PackageSourceTools
import com.njlabs.showjava.utils.rx.ProcessStatus
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class AppsHandler(private var context: Context) {

    fun loadApps(): Observable<ProcessStatus<ArrayList<PackageInfo>>> {
        return Observable.create { emitter: ObservableEmitter<ProcessStatus<ArrayList<PackageInfo>>> ->
            val installedApps = ArrayList<PackageInfo>()
            var packages = context.packageManager.getInstalledPackages(0)
            packages = packages.filter { pack ->
                !PackageSourceTools.isSystemPackage(pack)
            }
            packages.forEachIndexed { index, pack ->
                val packageInfo = PackageInfo()
                packageInfo.packageLabel =
                        pack.applicationInfo.loadLabel(context.packageManager).toString()
                packageInfo.packageName = pack.packageName
                packageInfo.packageVersion =
                        if (pack.versionName != null) pack.versionName else pack.versionCode.toString()
                packageInfo.packageFilePath = pack.applicationInfo.publicSourceDir
                packageInfo.packageIcon = pack.applicationInfo.loadIcon(context.packageManager)
                installedApps.add(packageInfo)
                val currentCount = index + 1

                emitter.onNext(
                    ProcessStatus(
                        (currentCount.toFloat() / packages.size.toFloat()) * 100f,
                        context.getString(R.string.loadingApp, packageInfo.packageLabel),
                        context.getString(R.string.loadingStatistic, currentCount, packages.size)
                    )
                )
            }
            installedApps.sortBy {
                it.packageLabel.toLowerCase()
            }
            emitter.onNext(ProcessStatus(installedApps))
            emitter.onComplete()
        }
    }
}