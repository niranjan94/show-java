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

package com.njlabs.showjava.activities.explorer.navigator

import android.content.Context
import com.njlabs.showjava.data.FileItem
import com.njlabs.showjava.utils.StringTools
import io.reactivex.Observable
import java.io.File
import java.text.DateFormat
import java.util.*

class NavigatorHandler(private var context: Context) {

    fun loadFiles(currentFile: File): Observable<ArrayList<FileItem>> {
        return Observable.fromCallable {
            val directories = ArrayList<FileItem>()
            val files = ArrayList<FileItem>()
            val items = currentFile.listFiles()
            items.forEach { file ->
                val lastModDate = DateFormat.getDateTimeInstance()
                    .format(
                        Date(
                            file.lastModified()
                        )
                    )
                if (file.isDirectory) {
                    val children = file.listFiles()
                    val noOfChildren = children?.size ?: 0
                    val fileSize = "$noOfChildren ${if (noOfChildren == 1) "item" else "items"}"
                    directories.add(FileItem(file, fileSize, lastModDate))
                } else {
                    val fileSize = StringTools.humanReadableByteCount(file.length(), true)
                    files.add(FileItem(file, fileSize, lastModDate))
                }
            }
            directories.sortBy { it.name?.toLowerCase() }
            files.sortBy { it.name?.toLowerCase() }
            directories.addAll(files)
            directories
        }
    }
}
