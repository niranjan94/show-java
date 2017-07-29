package com.njlabs.showjava.activites.landing

import android.content.Context
import android.os.Environment
import io.reactivex.Observable
import com.njlabs.showjava.models.SourceInfo
import com.njlabs.showjava.utils.Tools
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException

class LandingHandler(private var context: Context) {

    fun loadHistory(): Observable<ArrayList<SourceInfo>>? {
        return Observable.fromCallable {
            val historyItems = ArrayList<SourceInfo>()
            val showJavaDir = File("${Environment.getExternalStorageDirectory()}/ShowJava/")
            showJavaDir.mkdirs()
            val nomedia = File(showJavaDir, ".nomedia")
            if (!nomedia.exists() || !nomedia.isFile) {
                try {
                    nomedia.createNewFile()
                } catch (e: IOException) {
                    Timber.d(e)
                }

            }
            val dir = File("${Environment.getExternalStorageDirectory()}/ShowJava/sources")
            if (dir.exists()) {
                val files = dir.listFiles()
                if (files != null && files.isNotEmpty())
                    for (file in files) {
                        Timber.d(file.absolutePath)
                        if (Tools.sourceExists(file)) {
                            Tools.getSourceInfoFromSourcePath(file)?.let { historyItems.add(it) }
                        } else {
                            if (!Tools.isProcessorServiceRunning(context)) {
                                try {
                                    if (file.exists()) {
                                        if (file.isDirectory) {
                                            FileUtils.deleteDirectory(file)
                                        } else {
                                            file.delete()
                                        }
                                    }

                                } catch (e: Exception) {
                                    Timber.d(e)
                                }

                            }
                            if (file.exists() && !file.isDirectory) {
                                file.delete()
                            }
                        }
                    }
            }
            historyItems
        }
    }
}
