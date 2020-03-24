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

package com.njlabs.showjava.utils.ktx

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.njlabs.showjava.utils.SingletonHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class Storage(val context: Context) {
    val appStorage by lazy {
        getAppStorage(context)
    }

    fun sourceDir(packageName: String): File {
        return appStorage.resolve("sources/$packageName")
    }

    companion object : SingletonHolder<Storage, Context>(::Storage)
}

fun sourceDir(packageName: String): File {
    return Storage.getInstance().sourceDir(packageName)
}

/**
 * Get path to the primary storage directory on the user's internal memory
 */
private fun getAppStorage(context: Context?): File {
    if (isExternalStorageWritable()) {
        context!!.getExternalFilesDir("app-store")?.let { return it }
    }
    return File(context!!.filesDir, "show-java").resolve("app-store")
}

fun isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

/* Checks if external storage is available to at least read */
fun isExternalStorageReadable(): Boolean {
    return Environment.getExternalStorageState() in
            setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
}

fun getNameFromUri(context: Context, uri: Uri): String {
    val contentResolver: ContentResolver = context.contentResolver

    val nameFromResolver = contentResolver.query(uri, null, null, null, null).use { returnCursor ->
        returnCursor?.let {
            val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            returnCursor.getString(nameIndex)
        }
    }

    return if (nameFromResolver == null) {
        var nameFromPath: String = uri.path ?: "unknown"
        val cut: Int = nameFromPath.lastIndexOf('/')
        if (cut != -1) {
            nameFromPath = nameFromPath.substring(cut + 1)
        }
        nameFromPath
    } else {
        nameFromResolver
    }
}

suspend fun getFileFromContentUri(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    val fileName = getNameFromUri(context, uri)
    context.cacheDir.mkdirs()
    val filePath = context.cacheDir.resolve("${System.currentTimeMillis()}-${fileName}")
    context.contentResolver.openInputStream(uri)?.toFile(filePath)
    filePath
}
