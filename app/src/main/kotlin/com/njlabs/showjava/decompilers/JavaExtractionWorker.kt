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
import com.njlabs.showjava.R
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.utils.ZipUtils
import com.njlabs.showjava.utils.cleanMemory
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import org.apache.commons.io.FileUtils
import org.benf.cfr.reader.api.CfrDriver
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException


class JavaExtractionWorker(context: Context, data: Data) : BaseDecompiler(context, data) {

    @Throws(Exception::class)
    private fun decompileWithCFR(jarInputFiles: File, javaOutputDir: File) {
        cleanMemory()
        val jarFiles = jarInputFiles.listFiles()
        val options = mapOf<String, String>(
            "outputdir" to javaOutputDir.canonicalPath,
            "lomem" to "true"
        )
        val cfrDriver = CfrDriver.Builder().withOptions(options).build()
        cfrDriver.analyse(jarFiles.map { it.canonicalPath })
    }

    @Throws(Exception::class)
    private fun decompileWithJaDX(dexInputFiles: File, javaOutputDir: File) {
        cleanMemory()

        val args = JadxArgs()
        args.outDirSrc = javaOutputDir
        args.inputFiles = dexInputFiles.listFiles().toMutableList()
        args.threadsCount = 1

        val jadx = JadxDecompiler(args)
        jadx.load()
        jadx.saveSources()
        if (dexInputFiles.exists() && dexInputFiles.isDirectory) {
            dexInputFiles.deleteRecursively()
        }
    }

    @Throws(Exception::class)
    private fun decompileWithFernFlower(jarInputFiles: File, javaOutputDir: File) {
        cleanMemory()

        ConsoleDecompiler.main(
            arrayOf(
                jarInputFiles.canonicalPath, javaOutputDir.canonicalPath
            )
        )

        javaOutputDir.listFiles().forEach { decompiledJarFile ->
            if (decompiledJarFile.exists() && decompiledJarFile.isFile && decompiledJarFile.extension == "jar") {
                ZipUtils.unzip(decompiledJarFile, javaOutputDir, printStream!!)
                decompiledJarFile.delete()
            } else {
                throw FileNotFoundException("Decompiled jar does not exist")
            }
        }

    }

    override fun doWork(): ListenableWorker.Result {
        Timber.tag("JavaExtraction")
        context.getString(R.string.decompilingToJava).let {
            buildNotification(it)
            setStep(it)
        }

        super.doWork()

        val sourceInfo = SourceInfo.from(workingDirectory)
            .setPackageLabel(packageLabel)
            .setPackageName(packageName)
            .persist()

        try {
            when (decompiler) {
                "jadx" -> decompileWithJaDX(outputDexFiles, outputJavaSrcDirectory)
                "cfr" -> decompileWithCFR(outputJarFiles, outputJavaSrcDirectory)
                "fernflower" -> decompileWithFernFlower(outputJarFiles, outputJavaSrcDirectory)
            }
        } catch (e: Exception) {
            return exit(e)
        }

        if (outputDexFiles.exists() && outputDexFiles.isDirectory) {
            outputDexFiles.deleteRecursively()
        }

        if (outputJarFiles.exists() && outputJarFiles.isDirectory) {
            outputJarFiles.deleteRecursively()
        }

        sourceInfo
            .setJavaSourcePresence(true)
            .setSourceSize(FileUtils.sizeOfDirectory(workingDirectory))
            .persist()

        return successIf(outputJavaSrcDirectory.list().isNotEmpty())
    }
}
