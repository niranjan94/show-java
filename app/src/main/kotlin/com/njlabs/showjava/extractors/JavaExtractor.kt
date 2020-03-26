/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2020 Niranjan Rajendran
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

package com.njlabs.showjava.extractors

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import com.njlabs.showjava.R
import com.njlabs.showjava.data.SourceInfo
import com.njlabs.showjava.utils.ktx.cleanMemory
import org.apache.commons.io.FileUtils
import timber.log.Timber
import java.io.File

/**
 * The [JavaExtractor] does the actual decompilation of extracting the `java` source from
 * the inputs. All of the three decompiler that we support, allow passing in multiple input files.
 * So, we pass in all of the chunks of either dex (in case of JaDX) or jar files (in case of CFR &
 * fernflower) as the input.
 */
class JavaExtractor(context: Context, data: Data) : BaseExtractor(context, data) {

    /**
     * Do the decompilation using the CFR decompiler.
     *
     * We set the `lowmem` flag as true to let CFR know that it has to be more aggressive in terms
     * of garbage collection and less-aggressive caching. This results in reduced performance. But
     * increase success rates for large inputs. Which is a good trade-off.
     */
    @Throws(Exception::class)
    private fun decompileWithCFR(jarInputFiles: File, javaOutputDir: File) {
        cleanMemory()
        val jarFiles = jarInputFiles.listFiles()!!
        decompiler.extractSources(
            jarFiles.toList(), javaOutputDir
        )
    }

    /**
     * Do the decompilation using the JaDX decompiler.
     *
     * We set `threadsCount` as 1. This instructs JaDX to not spawn additional threads to prevent
     * issues on some devices.
     */
    @Throws(Exception::class)
    private fun decompileWithJaDX(dexInputFiles: File, javaOutputDir: File) {
        cleanMemory()
        decompiler.extractSources(
            dexInputFiles.listFiles()!!.toList(), javaOutputDir
        )

        if (dexInputFiles.exists() && dexInputFiles.isDirectory && !keepIntermediateFiles) {
            dexInputFiles.deleteRecursively()
        }
    }

    /**
     * Do the decompilation using FernFlower decompiler.
     *
     * The out of the decompiler is a jar archive containing the decompiled java files. So, we look
     * for and extract the archive after the decompilation.
     */
    @Throws(Exception::class)
    private fun decompileWithFernFlower(jarInputFiles: File, javaOutputDir: File) {
        cleanMemory()
        decompiler.extractSources(
            listOf(jarInputFiles), javaOutputDir
        )
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
            when (decompiler.name) {
                "jadx" -> decompileWithJaDX(outputDexFiles, outputJavaSrcDirectory)
                "cfr" -> decompileWithCFR(outputJarFiles, outputJavaSrcDirectory)
                "fernflower" -> decompileWithFernFlower(outputJarFiles, outputJavaSrcDirectory)
            }
        } catch (e: Exception) {
            return exit(e)
        }

        if (outputDexFiles.exists() && outputDexFiles.isDirectory && !keepIntermediateFiles) {
            outputDexFiles.deleteRecursively()
        }

        if (outputJarFiles.exists() && outputJarFiles.isDirectory && !keepIntermediateFiles) {
            outputJarFiles.deleteRecursively()
        }

        sourceInfo
            .setJavaSourcePresence(true)
            .setSourceSize(FileUtils.sizeOfDirectory(workingDirectory))
            .persist()

        return successIf(!outputJavaSrcDirectory.list().isNullOrEmpty())
    }
}
