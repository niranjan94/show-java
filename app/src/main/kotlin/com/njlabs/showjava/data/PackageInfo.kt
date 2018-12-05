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

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import com.njlabs.showjava.utils.Identicon
import com.njlabs.showjava.utils.getVersion
import com.njlabs.showjava.utils.isSystemPackage
import com.njlabs.showjava.utils.jarPackageName
import java.io.File


class PackageInfo() : Parcelable {
    var label = ""
    var name = ""
    var version = ""
    var filePath = ""
    var file: File = File("")
    var icon: Drawable? = null
    var type = Type.APK
    var isSystemPackage = false

    constructor(parcel: Parcel) : this() {
        label = parcel.readString()!!
        name = parcel.readString()!!
        version = parcel.readString()!!
        filePath = parcel.readString()!!
        type = Type.values()[parcel.readInt()]
        isSystemPackage = parcel.readInt() == 1
        file = File(filePath)
    }

    constructor(label: String, name: String, version: String, filePath: String, type: Type, isSystemPackage: Boolean = false) : this() {
        this.label = label
        this.name = name
        this.version = version
        this.filePath = filePath
        this.type = type
        this.isSystemPackage = isSystemPackage
        file = File(filePath)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label)
        parcel.writeString(name)
        parcel.writeString(version)
        parcel.writeString(filePath)
        parcel.writeInt(type.ordinal)
        parcel.writeInt(if (isSystemPackage) 1 else 0)
    }

    fun loadIcon(context: Context): Drawable {
        return when(type) {
            Type.APK -> context.packageManager.getPackageArchiveInfo(filePath, 0)
                .applicationInfo.loadIcon(context.packageManager)
            Type.JAR, Type.DEX ->
                BitmapDrawable(context.resources, Identicon.createFromObject(this.name + this.label))
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return String.format("filePath: %s", filePath)
    }

    enum class Type {
        APK, JAR, DEX
    }

    companion object CREATOR : Parcelable.Creator<PackageInfo> {

        /**
         * Get [PackageInfo] for an apk using the [context] and [android.content.pm.PackageInfo] instance.
         */
        fun fromApkPackageInfo(context: Context, pack: android.content.pm.PackageInfo): PackageInfo {
            return PackageInfo(
                pack.applicationInfo.loadLabel(context.packageManager).toString(),
                pack.packageName,
                getVersion(pack),
                pack.applicationInfo.publicSourceDir,
                Type.APK,
                isSystemPackage(pack)
            )
        }

        /**
         * Get [PackageInfo] for an apk using the [context] and the [file].
         */
        private fun fromApk(context: Context, file: File): PackageInfo {
            val pack = context.packageManager.getPackageArchiveInfo(file.canonicalPath, 0)
            return PackageInfo(
                pack.applicationInfo.loadLabel(context.packageManager).toString(),
                pack.packageName,
                getVersion(pack),
                file.canonicalPath,
                Type.APK,
                isSystemPackage(pack)
            )
        }

        /**
         * Get [PackageInfo] for a jar from the [file].
         */
        private fun fromJar(file: File, type: Type = Type.JAR): PackageInfo {
            return PackageInfo(
                file.name,
                jarPackageName(file.name),
                (System.currentTimeMillis() / 1000).toString(),
                file.canonicalPath,
                type
            )
        }

        /**
         * Get [PackageInfo] for a dex from the [file].
         */
        private fun fromDex(file: File): PackageInfo {
            return fromJar(file, Type.DEX)
        }


        /**
         * Get [PackageInfo] from a [file].
         */
        fun fromFile(context: Context, file: File): PackageInfo? {
            return when(file.extension) {
                "apk" -> fromApk(context, file)
                "jar" -> fromJar(file)
                "dex", "odex" -> fromDex(file)
                else -> null
            }
        }

        override fun createFromParcel(parcel: Parcel): PackageInfo {
            return PackageInfo(parcel)
        }

        override fun newArray(size: Int): Array<PackageInfo?> {
            return arrayOfNulls(size)
        }
    }
}
