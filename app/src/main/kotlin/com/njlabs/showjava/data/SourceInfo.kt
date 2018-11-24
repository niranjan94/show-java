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
import com.njlabs.showjava.utils.PackageSourceTools
import java.io.File

class SourceInfo() : Parcelable {

    lateinit var packageLabel: String
    lateinit var packageName: String
    var hasSource = true

    private var _sourceDirectory: File? = null

    val sourceDirectory: File
        get() {
            _sourceDirectory = _sourceDirectory ?: PackageSourceTools.sourceDir(packageName)
            return _sourceDirectory as File
        }

    constructor(parcel: Parcel) : this() {
        packageLabel = parcel.readString()!!
        packageName = parcel.readString()!!
        hasSource = parcel.readByte() != 0.toByte()
    }

    constructor(packageLabel: String, packageName: String) : this() {
        this.packageLabel = packageLabel
        this.packageName = packageName
    }

    constructor(hasSource: Boolean) : this() {
        this.hasSource = hasSource
    }

    fun hasSource(): Boolean {
        return hasSource
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(packageLabel)
        parcel.writeString(packageName)
        parcel.writeByte(if (hasSource) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SourceInfo> {
        override fun createFromParcel(parcel: Parcel): SourceInfo {
            return SourceInfo(parcel)
        }

        override fun newArray(size: Int): Array<SourceInfo?> {
            return arrayOfNulls(size)
        }
    }

}
