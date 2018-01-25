package com.njlabs.showjava.activities.explorer.navigator

import android.content.Context
import com.njlabs.showjava.models.FileItem
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
