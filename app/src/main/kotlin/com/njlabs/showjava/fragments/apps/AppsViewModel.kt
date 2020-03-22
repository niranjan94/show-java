/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2019 Niranjan Rajendran
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class AppsViewModel(application: Application): AndroidViewModel(application) {

    private val application: MainApplication = getApplication()

    /**
     * Load all installed applications.
     *
     * @return [Observable] which can be used to track the loading progress and completion state.
     */
    suspend fun loadApps(withSystemApps: Boolean, updateProgress: suspend (progress: Float, status: String, secondaryStatus: String) -> Unit): ArrayList<PackageInfo> {
        var installedApps = ArrayList<PackageInfo>()
        withContext(Dispatchers.IO) {
            var packages = application.packageManager.getInstalledPackages(0)
            packages = packages.filter { pack ->
                withSystemApps || !isSystemPackage(pack)
            }
            installedApps = ArrayList(packages.mapIndexed { index, pack ->
                val packageInfo = PackageInfo.fromApkPackageInfo(application, pack)
                packageInfo.icon = pack.applicationInfo.loadIcon(application.packageManager)
                val currentCount = index + 1
                updateProgress(
                    (currentCount.toFloat() / packages.size.toFloat()) * 100f,
                    application.getString(R.string.loadingApp, packageInfo.label),
                    application.getString(R.string.loadingStatistic, currentCount, packages.size)
                )
                packageInfo
            })
            installedApps.sortBy {
                it.label.toLowerCase(Locale.ROOT)
            }
        }
        return installedApps
    }

}