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

package com.njlabs.showjava.activities.landing

import android.content.Context
import android.os.Environment
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.utils.PackageSourceTools
import io.reactivex.Observable
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException

class LandingHandler(private var context: Context) {

    fun loadHistory(): Observable<ArrayList<SourceInfo>> {
        return Observable.fromCallable {
            val historyItems = ArrayList<SourceInfo>()
            val showJavaDir = File("${Environment.getExternalStorageDirectory()}/show-java/")
            showJavaDir.mkdirs()
            val nomedia = File(showJavaDir, ".nomedia")
            if (!nomedia.exists() || !nomedia.isFile) {
                try {
                    nomedia.createNewFile()
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
            val dir = File("${Environment.getExternalStorageDirectory()}/show-java/sources")
            if (dir.exists()) {
                val files = dir.listFiles()
                if (files != null && files.isNotEmpty())
                    files.forEach { file ->
                        Timber.d(file.canonicalPath)
                        if (PackageSourceTools.sourceExists(file)) {
                            PackageSourceTools.getSourceInfoFromSourcePath(file)
                                ?.let { historyItems.add(it) }
                        } else {
                            try {
                                if (file.exists()) {
                                    if (file.isDirectory) {
                                        FileUtils.deleteDirectory(file)
                                    } else {
                                        file.delete()
                                    }
                                }

                            } catch (e: Exception) {
                                Timber.d(e)
                            }
                            if (file.exists() && !file.isDirectory) {
                                file.delete()
                            }
                        }
                    }
            }
            historyItems
        }
    }
}
