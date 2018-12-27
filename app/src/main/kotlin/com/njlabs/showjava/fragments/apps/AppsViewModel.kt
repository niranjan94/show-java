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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.njlabs.showjava.MainApplication
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.utils.ktx.isSystemPackage
import com.njlabs.showjava.utils.rx.ProcessStatus
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class AppsViewModel(application: Application): AndroidViewModel(application) {

    private val application: MainApplication = getApplication()

    /**
     * Load all installed applications.
     *
     * @return [Observable] which can be used to track the loading progress and completion state.
     */
    fun loadApps(withSystemApps: Boolean): Observable<ProcessStatus<ArrayList<PackageInfo>>> {
        return Observable.create { emitter: ObservableEmitter<ProcessStatus<ArrayList<PackageInfo>>> ->
            val installedApps = ArrayList<PackageInfo>()
            var packages = application.packageManager.getInstalledPackages(0)
            packages = packages.filter { pack ->
                withSystemApps || !isSystemPackage(pack)
            }
            packages.forEachIndexed { index, pack ->
                val packageInfo = PackageInfo.fromApkPackageInfo(application, pack)
                packageInfo.icon = pack.applicationInfo.loadIcon(application.packageManager)
                packageInfo.isSystemPackage = isSystemPackage(pack)
                installedApps.add(packageInfo)
                val currentCount = index + 1
                emitter.onNext(
                    ProcessStatus(
                        (currentCount.toFloat() / packages.size.toFloat()) * 100f,
                        application.getString(R.string.loadingApp, packageInfo.label),
                        application.getString(R.string.loadingStatistic, currentCount, packages.size)
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