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
import androidx.work.Data
import androidx.work.ListenableWorker
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import com.njlabs.showjava.R
import com.njlabs.showjava.utils.PackageSourceTools
import com.njlabs.showjava.utils.ZipUtils
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.util.getopt.GetOptParser
import org.benf.cfr.reader.util.getopt.Options
import org.benf.cfr.reader.util.getopt.OptionsImpl
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.slf4j.Logger
import timber.log.Timber
import java.io.File
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import ch.qos.logback.core.FileAppender
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.Appender






class JavaExtractionWorker(context: Context, data: Data) : BaseDecompiler(context, data) {

    @Throws(Exception::class)
    private fun decompileWithCFR(jarInputFile: File, javaOutputDir: File) {
        val args = arrayOf(jarInputFile.toString(), "--outputdir", javaOutputDir.toString())

        val getOptParser = GetOptParser()
        val options: Options?
        val files: List<String?>?

        try {
            val processedArgs = getOptParser.parse(args, OptionsImpl.getFactory())
            files = processedArgs.first as List<String>
            options = processedArgs.second as Options
            if (files.isEmpty()) {
                return sendStatus("exit_process_on_error")
            }
        } catch (e: Exception) {
            return sendStatus("exit_process_on_error")
        }

        if (!options.optionIsSet(OptionsImpl.HELP) && !files.isEmpty()) {
            val cfrDriver = CfrDriver.Builder().withBuiltOptions(options).build()
            cfrDriver.analyse(files)
        } else {
            sendStatus("exit_process_on_error")
        }
    }

    @Throws(Exception::class)
    private fun decompileWithJaDX(dexInputFile: File, javaOutputDir: File) {
        val args = JadxArgs()
        args.outDirSrc = javaOutputDir
        args.inputFiles = mutableListOf(dexInputFile)
        args.threadsCount = 1

        val jadx = JadxDecompiler(args)
        jadx.load()
        jadx.saveSources()
        if (dexInputFile.exists() && dexInputFile.isFile) {
            dexInputFile.delete()
        }
    }

    @Throws(Exception::class)
    private fun decompileWithFernFlower(jarInputFile: File, javaOutputDir: File) {
        ConsoleDecompiler.main(
            arrayOf(
                jarInputFile.canonicalPath, javaOutputDir.canonicalPath
            )
        )
        if (outputJarFile.exists()) {
            ZipUtils.unzip(outputJarFile, javaOutputDir, printStream!!)
            outputJarFile.delete()
        } else {
            sendStatus("exit_process_on_error")
        }
    }

    override fun doWork(): ListenableWorker.Result {
        Timber.tag("JavaExtraction")
        buildNotification(context.getString(R.string.decompilingToJava))

        super.doWork()

        sendStatus("jar2java")

        if (decompiler != "jadx") {
            if (outputDexFile.exists() && outputDexFile.isFile) {
                outputDexFile.delete()
            }
        }

        PackageSourceTools.initialise(packageLabel, packageName, workingDirectory.canonicalPath)

        try {
            when (decompiler) {
                "jadx" -> decompileWithJaDX(outputDexFile, outputJavaSrcDirectory)
                "cfr" -> decompileWithCFR(outputJarFile, outputJavaSrcDirectory)
                "fernflower" -> decompileWithFernFlower(outputJarFile, outputJavaSrcDirectory)
            }
        } catch (e: Exception) {
            return exit(e)
        }

        PackageSourceTools.setJavaSourceStatus(workingDirectory.canonicalPath, false)
        return ListenableWorker.Result.SUCCESS
    }
}
