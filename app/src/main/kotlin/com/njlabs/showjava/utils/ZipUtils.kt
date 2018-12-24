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


import com.njlabs.showjava.utils.ktx.appStorage
import timber.log.Timber
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * [ZipUtils] to extract and compress directories
 */
object ZipUtils {

    fun zipDir(dir: File, packageId: String): File {
        val zipIntoDir = appStorage.resolve("archives")
        if (!zipIntoDir.exists() || !zipIntoDir.isDirectory) {
            zipIntoDir.mkdirs()
        }
        val zipFile = File(zipIntoDir, "$packageId.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }
        try {
            val zip: ZipOutputStream
            val fileWriter = FileOutputStream(zipFile)
            zip = ZipOutputStream(fileWriter)
            addFolderToZip("", dir.toString(), zip)
            zip.flush()
            zip.close()
        } catch (e: Exception) {
            Timber.e(e)
        }

        return zipFile
    }

    @Throws(Exception::class)
    private fun addFileToZip(path: String, srcFile: String, zip: ZipOutputStream) {
        val folder = File(srcFile)
        if (folder.isDirectory) {
            addFolderToZip(path, srcFile, zip)
        } else {
            val buffer = ByteArray(1024)
            val fileInputStream = FileInputStream(srcFile)
            zip.putNextEntry(ZipEntry(path + "/" + folder.name))

            while (true) {
                val len = fileInputStream.read(buffer)
                if (len <= 0) {
                    break
                }
                zip.write(buffer, 0, len)
            }
        }
    }

    @Throws(Exception::class)
    private fun addFolderToZip(path: String, srcFolder: String, zip: ZipOutputStream) {
        val folder = File(srcFolder)
        for (fileName in folder.list()) {
            if (path == "") {
                addFileToZip(folder.name, "$srcFolder/$fileName", zip)
            } else {
                addFileToZip(path + "/" + folder.name, "$srcFolder/$fileName", zip)
            }
        }
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun unzip(zipFile: File, targetDirectory: File, printStream: PrintStream = System.out) {
        val zipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(zipFile)))
        zipInputStream.use { zis ->
            val buffer = ByteArray(8192)
            while (true) {
                val zipEntry = zis.nextEntry ?: break
                val file = File(targetDirectory, zipEntry.name)
                printStream.println(zipEntry.name)
                val dir = if (zipEntry.isDirectory) file else file.parentFile
                if (!dir.isDirectory && !dir.mkdirs()) {
                    throw FileNotFoundException("Failed to ensure directory: " + dir.canonicalPath)
                }

                if (zipEntry.isDirectory) {
                    continue
                }

                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.use { out ->
                    while (true) {
                        val count = zis.read(buffer)
                        if (count <= 0) {
                            break
                        }
                        out.write(buffer, 0, count)
                    }
                }
                val time = zipEntry.time
                if (time > 0) {
                    file.setLastModified(time)
                }
            }
        }
    }
}