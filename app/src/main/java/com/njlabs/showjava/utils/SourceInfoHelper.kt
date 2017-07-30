package com.njlabs.showjava.utils

import com.njlabs.showjava.models.SourceInfo
import org.apache.commons.io.FileUtils
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException

class SourceInfoHelper {
    companion object {
        fun initialise(packageLabel: String, packageName: String, sourceOutputDir: String) {
            try {
                val json = JSONObject()
                json.put("package_label", packageLabel)
                json.put("package_name", packageName)
                json.put("has_java_sources", false)
                json.put("has_xml_sources", false)
                val filePath = sourceOutputDir + "/info.json"
                FileUtils.writeStringToFile(File(filePath), json.toString(), "UTF-8")
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        fun setjavaSourceStatus(sourceOutputDir: String, status: Boolean?) {
            try {
                val infoFile = File(sourceOutputDir + "/info.json")
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
                val infoFile = File(sourceOutputDir + "/info.json")
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
                val infoFile = File(sourceOutputDir + "/info.json")
                infoFile.delete()
            } catch (e: Exception) {
                Timber.e(e)
            }

        }

        fun getLabel(directory: String): String? {
            try {
                val infoFile = File(directory + "/info.json")
                val json = JSONObject(FileUtils.readFileToString(infoFile, "UTF-8"))
                return json.getString("package_label")
            } catch (e: IOException) {
                return null
            } catch (e: JSONException) {
                return null
            }

        }

        fun getSourceInfo(infoFile: File): SourceInfo? {
            try {
                val json = JSONObject(FileUtils.readFileToString(infoFile, "UTF-8"))
                if (json.getBoolean("has_java_sources") || json.getBoolean("has_xml_sources")) {
                    return SourceInfo(json.getString("package_label"), json.getString("package_name"))
                } else {
                    return null
                }
            } catch (e: IOException) {
                return null
            } catch (e: JSONException) {
                return null
            }

        }

    }
}