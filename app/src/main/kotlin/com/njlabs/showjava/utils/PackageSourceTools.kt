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

package com.njlabs.showjava.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Environment
import com.njlabs.showjava.data.SourceInfo
import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException

object PackageSourceTools {

    private val appStoragePath: String
        get() {
            return "${Environment.getExternalStorageDirectory()}/show-java/"
        }

    fun getPath(relativePath: String): String {
        return appStoragePath + (if (!relativePath.startsWith("/")) "/" else "") + relativePath
    }

    fun isSystemPackage(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    fun sourceDir(packageName: String): File {
        return File(getPath("sources/$packageName"))
    }

    fun initialise(packageLabel: String, packageName: String, sourceOutputDir: String) {
        try {
            val json = JSONObject()
            json.put("package_label", packageLabel)
            json.put("package_name", packageName)
            json.put("has_java_sources", false)
            json.put("has_xml_sources", false)
            val filePath = "$sourceOutputDir/info.json"
            FileUtils.writeStringToFile(File(filePath), json.toString(), "UTF-8")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun setJavaSourceStatus(sourceOutputDir: String, status: Boolean?) {
        try {
            val infoFile = File("$sourceOutputDir/info.json")
            val json = JSONObject(FileUtils.readFileToString(infoFile, "UTF-8"))
            json.put("has_java_sources", status)
            FileUtils.writeStringToFile(infoFile, json.toString(), "UTF-8")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun setXmlSourceStatus(sourceOutputDir: String, status: Boolean?) {
        try {
            val infoFile = File("$sourceOutputDir/info.json")
            val json = JSONObject(FileUtils.readFileToString(infoFile, "UTF-8"))
            json.put("has_xml_sources", status)
            FileUtils.writeStringToFile(infoFile, json.toString(), "UTF-8")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun delete(sourceOutputDir: String) {
        try {
            val infoFile = File("$sourceOutputDir/info.json")
            infoFile.delete()
        } catch (e: Exception) {
            Timber.e(e)
        }

    }

    fun getLabel(directory: String): String? {
        return try {
            val infoFile = File("$directory/info.json")
            val json = JSONObject(FileUtils.readFileToString(infoFile, "UTF-8"))
            json.getString("package_label")
        } catch (e: IOException) {
            null
        } catch (e: JSONException) {
            null
        }

    }

    fun getSourceInfo(infoFile: File): SourceInfo? {
        return try {
            val json = JSONObject(FileUtils.readFileToString(infoFile, "UTF-8"))
            if (json.getBoolean("has_java_sources") || json.getBoolean("has_xml_sources")) {
                SourceInfo(json.getString("package_label"), json.getString("package_name"))
            } else {
                null
            }
        } catch (e: IOException) {
            null
        } catch (e: JSONException) {
            null
        }

    }

    fun sourceExists(sourceDir: File): Boolean {
        if (sourceDir.exists() && sourceDir.isDirectory) {
            val infoFile = File(sourceDir.toString() + "/info.json")
            if (infoFile.exists() && infoFile.isFile) {
                val sourceInfo = PackageSourceTools.getSourceInfo(infoFile)
                if (sourceInfo != null) {
                    return true
                }
            }
        }
        return false
    }

    fun getSourceInfoFromSourcePath(sourceDir: File): SourceInfo? {
        if (sourceDir.isDirectory) {
            val infoFile = File(sourceDir.toString() + "/info.json")
            if (infoFile.exists() && infoFile.isFile) {
                return PackageSourceTools.getSourceInfo(infoFile)
            }
        }
        return null
    }
}