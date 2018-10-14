import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.WorkerParameters
import com.njlabs.showjava.workers.decompiler.BaseWorker
import com.njlabs.showjava.utils.PackageSourceTools
import jadx.api.JadxDecompiler
import net.dongliu.apk.parser.ApkFile
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipFile


class ResourcesExtractionWorker(context : Context, params : WorkerParameters) : BaseWorker(context, params) {

    private var parsedInputApkFile = ApkFile(inputPackageFile)

    private fun extractResourcesWithJadx() {
        try {
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
        } catch (e: Exception) {
            sendStatus("start_activity_with_error")
        } catch (e: StackOverflowError) {
            sendStatus("start_activity_with_error")
        }
    }

    private fun extractResourcesWithParser() {
        try {
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
        } catch (e: Exception) {
            sendStatus("start_activity_with_error")
        } catch (e: StackOverflowError) {
            sendStatus("start_activity_with_error")
        }
    }

    private fun writeFile(fileStream: InputStream, path: String) {
        var outputStream: FileOutputStream? = null
        try {
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
            outputStream = FileOutputStream(File(fileFolderPath + FilenameUtils.getName(path)))
            val buffer = ByteArray(1024)
            while (true) {
                val read = fileStream.read(buffer)
                if (read <= 0) {
                    break
                }
                outputStream.write(buffer, 0, read)
            }
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            try {
                fileStream.close()
            } catch (e: IOException) {
                Timber.e(e)
            }
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    Timber.e(e)
                }

            }
        }
    }

    private fun writeXML(path: String) {
        try {
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
        } catch (e: IOException) {
            Timber.e(e)
        }

    }

    private fun writeManifest() {
        try {
            val manifestXml = parsedInputApkFile.manifestXml
            FileUtils.writeStringToFile(
                File(workingDirectory.canonicalPath, "AndroidManifest.xml"),
                manifestXml,
                Charset.defaultCharset()
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveIcon() {
        try {
            val icon = parsedInputApkFile.iconFile.data ?: return
            val bitmap = BitmapFactory.decodeByteArray(icon, 0, icon.size)
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(File(workingDirectory.canonicalPath, "icon.png"))
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                try {
                    out?.close()
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    override fun doWork(): Result {
        Timber.tag("ResourcesExtraction")
        super.doWork()

        when (decompiler) {
            "jadx" -> extractResourcesWithJadx()
            else -> extractResourcesWithParser()
        }

        saveIcon()
        PackageSourceTools.setXmlSourceStatus(workingDirectory.canonicalPath, true)

        return Result.SUCCESS
    }
}
