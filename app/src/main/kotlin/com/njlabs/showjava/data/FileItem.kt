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

package com.njlabs.showjava.data

import android.os.Parcel
import android.os.Parcelable
import com.njlabs.showjava.R
import org.apache.commons.io.FilenameUtils
import java.io.File

/**
 * Each file/folder item displayed in [com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity]
 */
class FileItem() : Parcelable {

    var file: File = File("/")
    var fileSize: String = ""
    var metaInfo: String? = ""

    val name: String?
        get() = if (metaInfo == "parent") ".." else file.name

    /**
     * Returns the appropriate icon resource based on the file extension via reflection
     */
    val iconResource: Int
        get() {
            if (metaInfo == "parent") {
                return R.drawable.previous
            }
            if (file.isDirectory) {
                return R.drawable.type_folder
            }
            var extension = FilenameUtils.getExtension(file.name) ?: return R.drawable.type_file
            extension = if (extension === "jpeg") "jpg" else extension
            return try {
                val res = R.drawable::class.java
                val drawableField = res.getField("type_$extension")
                drawableField.getInt(null)
            } catch (e: Exception) {
                R.drawable.type_file
            }
        }


    constructor(parcel: Parcel) : this() {
        file = File(parcel.readString())
        fileSize = parcel.readString() as String
        metaInfo = parcel.readString() as String
    }

    constructor(file: File, fileSize: String, metaInfo: String) : this() {
        this.file = file
        this.fileSize = fileSize
        this.metaInfo = metaInfo
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(file.canonicalPath)
        parcel.writeString(fileSize)
        parcel.writeString(metaInfo)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileItem> {
        override fun createFromParcel(parcel: Parcel): FileItem {
            return FileItem(parcel)
        }

        override fun newArray(size: Int): Array<FileItem?> {
            return arrayOfNulls(size)
        }
    }
}