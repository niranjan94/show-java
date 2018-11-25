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

package com.njlabs.showjava.decompilers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.work.Data
import androidx.work.ListenableWorker
import com.njlabs.showjava.R
import com.njlabs.showjava.utils.PackageSourceTools
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import net.dongliu.apk.parser.ApkFile
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipFile


class ResourcesExtractionWorker(context: Context, data: Data) : BaseDecompiler(context, data) {

    private var parsedInputApkFile = ApkFile(inputPackageFile)
    private val images = listOf("jpg", "png", "gif", "jpeg", "webp", "tiff", "bmp")

    @Throws(Exception::class)
    private fun extractResourcesWithJadx() {
        val resDir = outputResSrcDirectory
        val args = JadxArgs()
        args.outDirRes = resDir
        args.inputFiles = mutableListOf(inputPackageFile)
        val jadx = JadxDecompiler(args)
        jadx.load()
        jadx.saveResources()
        val zipFile = ZipFile(inputPackageFile)
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            if (!zipEntry.isDirectory && FilenameUtils.isExtension(zipEntry.name, images)) {
                sendStatus(zipEntry.name)
                writeFile(zipFile.getInputStream(zipEntry), zipEntry.name)
            }
        }
        zipFile.close()
    }

    @Throws(Exception::class)
    private fun extractResourcesWithParser() {
        val zipFile = ZipFile(inputPackageFile)
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            if (!zipEntry.isDirectory
                && zipEntry.name != "AndroidManifest.xml"
                && FilenameUtils.isExtension(zipEntry.name, "xml")) {
                sendStatus(zipEntry.name)
                writeXML(zipEntry.name)
            } else if (!zipEntry.isDirectory && FilenameUtils.isExtension(zipEntry.name, images)) {
                sendStatus(zipEntry.name)
                writeFile(zipFile.getInputStream(zipEntry), zipEntry.name)
            }
        }
        zipFile.close()
        writeManifest()
    }

    @Throws(Exception::class)
    private fun writeFile(fileStream: InputStream, path: String) {
        val fileFolderPath =
            outputResSrcDirectory.canonicalPath + "/" + path.replace(
                FilenameUtils.getName(
                    path
                ), ""
            )
        val fileFolder = File(fileFolderPath)
        if (!fileFolder.exists() || !fileFolder.isDirectory) {
            fileFolder.mkdirs()
        }
        val outputStream = FileOutputStream(File(fileFolderPath + FilenameUtils.getName(path)))
        val buffer = ByteArray(1024)
        while (true) {
            val read = fileStream.read(buffer)
            if (read <= 0) {
                break
            }
            outputStream.write(buffer, 0, read)
        }
        fileStream.close()
        outputStream.close()
    }

    @Throws(Exception::class)
    private fun writeXML(path: String) {
        val xml = parsedInputApkFile.transBinaryXml(path)
        val fileFolderPath =
            outputResSrcDirectory.canonicalPath + "/" + path.replace(
                FilenameUtils.getName(
                    path
                ), ""
            )
        val fileFolder = File(fileFolderPath)
        if (!fileFolder.exists() || !fileFolder.isDirectory) {
            fileFolder.mkdirs()
        }
        FileUtils.writeStringToFile(
            File(fileFolderPath + FilenameUtils.getName(path)),
            xml,
            Charset.defaultCharset()
        )
    }

    @Throws(Exception::class)
    private fun writeManifest() {
        val manifestXml = parsedInputApkFile.manifestXml
        FileUtils.writeStringToFile(
            File(workingDirectory.canonicalPath, "AndroidManifest.xml"),
            manifestXml,
            Charset.defaultCharset()
        )
    }

    // Borrowed from from https://stackoverflow.com/a/52453231/1562480
    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    @Throws(Exception::class)
    private fun saveIcon() {
        val packageInfo = context.packageManager.getPackageArchiveInfo(inputPackageFile.canonicalPath, 0)
        val bitmap = getBitmapFromDrawable(packageInfo.applicationInfo.loadIcon(context.packageManager))
        val iconOutput = FileOutputStream(File(workingDirectory.canonicalPath, "icon.png"))
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, iconOutput)
        iconOutput.close()
    }

    override fun doWork(): ListenableWorker.Result {
        Timber.tag("ResourcesExtraction")
        context.getString(R.string.extractingResources).let {
            buildNotification(it)
            setStep(it)
        }

        super.doWork()

        /* when (decompiler) {
            "jadx" -> extractResourcesWithJadx()
            else -> extractResourcesWithParser()
        } */

        // Not using JaDX for resource extraction.
        // Due to its dependency on the javax.imageio.ImageIO class which is unavailable on android
        extractResourcesWithParser()

        saveIcon()
        PackageSourceTools.setXmlSourceStatus(workingDirectory.canonicalPath, true)

        return ListenableWorker.Result.SUCCESS
    }
}
