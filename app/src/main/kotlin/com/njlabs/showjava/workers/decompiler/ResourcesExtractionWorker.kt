package com.njlabs.showjava.workers.decompiler

import android.content.Context
import android.graphics.Bitmap
import androidx.work.WorkerParameters
import com.njlabs.showjava.R
import com.njlabs.showjava.utils.PackageSourceTools
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
import java.lang.Exception
import android.graphics.drawable.BitmapDrawable



class ResourcesExtractionWorker(context : Context, params : WorkerParameters) : BaseWorker(context, params) {

    private var parsedInputApkFile = ApkFile(inputPackageFile)

    @Throws(Exception::class)
    private fun extractResourcesWithJadx() {
        val resDir = outputResSrcDirectory
        val jadx = JadxDecompiler()
        jadx.setOutputDir(resDir)
        jadx.loadFile(inputPackageFile)
        jadx.saveResources()
        val zipFile = ZipFile(inputPackageFile)
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            if (!zipEntry.isDirectory && (FilenameUtils.getExtension(zipEntry.name) == "png" || FilenameUtils.getExtension(
                    zipEntry.name
                ) == "jpg")) {
                sendStatus("progress_stream", zipEntry.name)
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
            if (!zipEntry.isDirectory && zipEntry.name != "AndroidManifest.xml" && FilenameUtils.getExtension(
                    zipEntry.name
                ) == "xml") {
                sendStatus("progress_stream", zipEntry.name)
                writeXML(zipEntry.name)
            } else if (!zipEntry.isDirectory && (FilenameUtils.getExtension(zipEntry.name) == "png" || FilenameUtils.getExtension(
                    zipEntry.name
                ) == "jpg")) {
                sendStatus("progress_stream", zipEntry.name)
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

    @Throws(Exception::class)
    private fun saveIcon() {
        val packageInfo = context.packageManager.getPackageArchiveInfo(inputPackageFile.canonicalPath, 0)
        val bitmap = (packageInfo.applicationInfo.loadIcon(context.packageManager) as BitmapDrawable).bitmap
        val iconOutput = FileOutputStream(File(workingDirectory.canonicalPath, "icon.png"))
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, iconOutput)
        iconOutput.close()
    }

    override fun doWork(): Result {
        Timber.tag("ResourcesExtraction")
        buildNotification(context.getString(R.string.extractingResources))

        super.doWork()

        when (decompiler) {
            "jadx" -> extractResourcesWithJadx()
            else -> extractResourcesWithParser()
        }

        // saveIcon()
        PackageSourceTools.setXmlSourceStatus(workingDirectory.canonicalPath, true)

        return Result.SUCCESS
    }
}
