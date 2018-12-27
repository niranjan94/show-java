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

package com.njlabs.showjava.fragments.landing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.utils.ktx.appStorage
import io.reactivex.Observable
import timber.log.Timber
import java.io.File
import java.io.IOException

class LandingViewModel(application: Application): AndroidViewModel(application) {

    fun loadHistory(): Observable<ArrayList<SourceInfo>> {
        return Observable.fromCallable {
            val historyItems = ArrayList<SourceInfo>()
            appStorage.mkdirs()
            val nomedia = File(appStorage, ".nomedia")
            if (!nomedia.exists() || !nomedia.isFile) {
                try {
                    nomedia.createNewFile()
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
            val sourcesDir = appStorage.resolve("sources")
            if (sourcesDir.exists()) {
                val files = sourcesDir.listFiles()
                if (files != null && files.isNotEmpty())
                    files.forEach { file ->
                        if (SourceInfo.exists(file)) {
                            SourceInfo.from(file).let {
                                historyItems.add(it)
                            }
                        }
                    }
            }
            historyItems
        }
    }

}