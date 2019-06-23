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

package com.njlabs.showjava.data

import android.os.Parcel
import android.os.Parcelable
import com.njlabs.showjava.utils.ktx.getDate
import com.njlabs.showjava.utils.ktx.sourceDir
import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * [SourceInfo] holds information about a specific decompiled source on the app storage.
 * The source info is persisted to disk as a simple json file at the root of each package's
 * decompiled source folder.
 */
class SourceInfo() : Parcelable {

    var packageLabel: String = "Unknown package"
    var packageName: String = "unknown.package"

    var hasJavaSource: Boolean = false
    var hasXmlSource: Boolean = false

    var createdAt: String = getDate()
    var updatedAt: String = getDate()

    var sourceSize: Long = 0

    lateinit var sourceDirectory: File

    constructor(parcel: Parcel) : this() {
        packageLabel = parcel.readString()!!
        packageName = parcel.readString()!!
        hasJavaSource = parcel.readInt() == 1
        hasXmlSource = parcel.readInt() == 1
        createdAt = parcel.readString()!!
        updatedAt = parcel.readString()!!
        sourceSize = parcel.readLong()
        sourceDirectory = sourceDir(packageName)
    }

    constructor(packageLabel: String, packageName: String, hasJavaSource: Boolean, hasXmlSource:Boolean, createdAt: String, updatedAt: String, sourceSize: Long) : this() {
        this.packageLabel = packageLabel
        this.packageName = packageName
        this.hasJavaSource = hasJavaSource
        this.hasXmlSource = hasXmlSource
        this.createdAt = createdAt
        this.updatedAt = updatedAt
        this.sourceSize = sourceSize
        sourceDirectory = sourceDir(packageName)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(packageLabel)
        parcel.writeString(packageName)
        parcel.writeInt(if (hasJavaSource) 1 else 0)
        parcel.writeInt(if (hasXmlSource) 1 else 0)
        parcel.writeString(createdAt)
        parcel.writeString(updatedAt)
        parcel.writeLong(sourceSize)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setPackageLabel(packageLabel: String): SourceInfo {
        this.packageLabel = packageLabel
        return this
    }

    fun setPackageName(packageName: String): SourceInfo {
        this.packageName = packageName
        this.sourceDirectory = sourceDir(packageName)
        return this
    }

    fun setJavaSourcePresence(hasJavaSource: Boolean): SourceInfo {
        this.hasJavaSource = hasJavaSource
        return this
    }

    fun setXmlSourcePresence(hasXmlSource: Boolean): SourceInfo {
        this.hasXmlSource = hasXmlSource
        return this
    }

    fun setSourceSize(sourceSize: Long): SourceInfo {
        this.sourceSize = sourceSize
        return this
    }

    fun persist(): SourceInfo {
        synchronized(this) {
            updatedAt = getDate()
            try {
                val infoFile = getInfoFile(sourceDirectory)
                val json = JSONObject()
                json.put("package_label", packageLabel)
                json.put("package_name", packageName)
                json.put("has_java_sources", hasJavaSource)
                json.put("has_xml_sources", hasXmlSource)
                json.put("created_at", createdAt)
                json.put("updated_at", updatedAt)
                json.put("source_size", sourceSize)
                FileUtils.writeStringToFile(
                    infoFile,
                    json.toString(2),
                    "UTF-8"
                )
            } catch (e: IOException) {
                Timber.e(e)
            } catch (e: JSONException) {
                Timber.e(e)
            }
            return this
        }
    }

    fun exists(): Boolean {
        return hasJavaSource || hasXmlSource
    }

    companion object CREATOR : Parcelable.Creator<SourceInfo> {
        private fun getInfoFile(source: File): File {
            if (!source.exists() || source.isFile) {
                return source
            }
            return source.resolve("info.json")
        }

        fun exists(sourceDir: File): Boolean {
            if (sourceDir.exists() && sourceDir.isDirectory) {
                val infoFile = sourceDir.resolve("info.json")
                if (infoFile.exists() && infoFile.isFile) {
                    val sourceInfo = from(sourceDir)
                    if (sourceInfo.hasJavaSource || sourceInfo.hasXmlSource) {
                        return true
                    }
                }
            }
            return false
        }

        fun from(source: File): SourceInfo {
            return try {
                val infoFile = getInfoFile(source)
                if (!infoFile.exists()) {
                    return SourceInfo()
                }
                val json = JSONObject(FileUtils.readFileToString(infoFile, "UTF-8"))
                if (json.getBoolean("has_java_sources") || json.getBoolean("has_xml_sources")) {
                    SourceInfo(
                        json.getString("package_label"),
                        json.getString("package_name"),
                        json.getBoolean("has_java_sources"),
                        json.getBoolean("has_xml_sources"),
                        json.getString("created_at"),
                        json.getString("updated_at"),
                        json.getLong("source_size")
                    )
                } else {
                    SourceInfo()
                }
            } catch (e: IOException) {
                SourceInfo()
            } catch (e: JSONException) {
                SourceInfo()
            }
        }

        override fun createFromParcel(parcel: Parcel): SourceInfo {
            return SourceInfo(parcel)
        }
        override fun newArray(size: Int): Array<SourceInfo?> {
            return arrayOfNulls(size)
        }
    }
}
