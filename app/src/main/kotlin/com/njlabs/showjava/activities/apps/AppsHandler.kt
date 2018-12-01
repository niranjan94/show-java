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

package com.njlabs.showjava.activities.apps

import android.content.Context
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.utils.getVersion
import com.njlabs.showjava.utils.isSystemPackage
import com.njlabs.showjava.utils.rx.ProcessStatus
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class AppsHandler(private var context: Context) {

    fun loadApps(): Observable<ProcessStatus<ArrayList<PackageInfo>>> {
        return Observable.create { emitter: ObservableEmitter<ProcessStatus<ArrayList<PackageInfo>>> ->
            val installedApps = ArrayList<PackageInfo>()
            var packages = context.packageManager.getInstalledPackages(0)
            packages = packages.filter { pack ->
                !isSystemPackage(pack)
            }
            packages.forEachIndexed { index, pack ->
                val packageInfo = PackageInfo()
                packageInfo.label =
                        pack.applicationInfo.loadLabel(context.packageManager).toString()
                packageInfo.name = pack.packageName
                packageInfo.version = getVersion(pack)
                packageInfo.filePath = pack.applicationInfo.publicSourceDir
                packageInfo.icon = pack.applicationInfo.loadIcon(context.packageManager)
                installedApps.add(packageInfo)
                val currentCount = index + 1
                emitter.onNext(
                    ProcessStatus(
                        (currentCount.toFloat() / packages.size.toFloat()) * 100f,
                        context.getString(R.string.loadingApp, packageInfo.label),
                        context.getString(R.string.loadingStatistic, currentCount, packages.size)
                    )
                )
            }
            installedApps.sortBy {
                it.label.toLowerCase()
            }
            emitter.onNext(ProcessStatus(installedApps))
            emitter.onComplete()
        }
    }
}