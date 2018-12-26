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
import com.googlecode.dex2jar.Method
import com.googlecode.dex2jar.ir.IrMethod
import com.googlecode.dex2jar.reader.DexFileReader
import com.googlecode.dex2jar.v3.Dex2jar
import com.googlecode.dex2jar.v3.DexExceptionHandler
import com.njlabs.showjava.R
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.utils.UserPreferences
import com.njlabs.showjava.utils.ktx.cleanMemory
import com.njlabs.showjava.utils.ktx.toClassName
import org.apache.commons.io.FilenameUtils
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.dexbacked.DexBackedOdexFile
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.immutable.ImmutableDexFile
import org.objectweb.asm.tree.MethodNode
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * The [JarExtractionWorker] worker handles optimization and extraction of the jar/dex file from the source
 *
 * a. For APKs & Dex files:
 *      1. All the classes within the input are read
 *      2. Classes are checked against the ignore list.
 *      3. Multiple dex files are created with the classes that are not ignored.
 *      4. For inputs with a large number of classes, creating a single dex file causes devices to
 *         run out of memory. So, we chunk them to smaller dex files containing lesser number
 *         of classes.
 *      5. The dex files are then converted to jar. (With the exception of JaDX which prefers dex files)
 *
 * b. For Jar files:
 *      1. They are directly passed to the next step. (With the exception of JaDX where the jar is
 *         read and converted into dex chunks and then passed on to JaDX)
 */
class JarExtractionWorker(context: Context, data: Data) : BaseDecompiler(context, data) {

    private var ignoredLibs: ArrayList<String> = ArrayList()

    // Set a lower max memory than available.
    // Creating a dex file can easily use up memory before we get a chance to exit gracefully
    override val maxMemoryAdjustmentFactor = 1.5

    /**
     * Load a list of library classes that are to be ignored from the assets
     */
    private fun loadIgnoredLibs() {
        context.assets.open("ignored.basic.list").bufferedReader().useLines {
            it.forEach { line -> ignoredLibs.add(toClassName(line)) }
        }
        if (data.getBoolean("shouldIgnoreLibs", true)) {
            context.assets.open("ignored.list").bufferedReader().useLines {
                it.forEach { line -> ignoredLibs.add(toClassName(line)) }
            }
            if (!packageName.contains("facebook", true)) {
                ignoredLibs.add(toClassName("com.facebook"))
            }
        }
        Timber.d("Total libs to ignore: ${ignoredLibs.size}")
    }

    /**
     * Check if the given class name is in the ignore-list
     */
    private fun isIgnored(className: String): Boolean {
        return ignoredLibs.any { className.startsWith(it) }
    }

    /**
     * Convert the apk to a dex file.
     * During the generation of the dex-file, any ignored classes will be stripped.
     */
    @Throws(Exception::class)
    private fun convertApkToDex() {
        cleanMemory()

        Timber.i("Starting APK to DEX Conversion")
        sendStatus(context.getString(R.string.optimizing))

        val classes = ArrayList<ClassDef>()

        fun addClassesFromDex(inputStream: InputStream, type: String = "dex") {
            val dexFile =
                if (type == "dex")
                    DexBackedDexFile.fromInputStream(
                        Opcodes.getDefault(),
                        BufferedInputStream(inputStream)
                    )
                else
                    DexBackedOdexFile.fromInputStream(
                        Opcodes.getDefault(),
                        BufferedInputStream(inputStream)
                    )
            for (classDef in dexFile.classes) {
                if (!isIgnored(classDef.type)) {
                    val currentClass = classDef.type
                    sendStatus(
                        context.getString(R.string.optimizingClasses),
                        currentClass.replace("Processing ", "")
                    )
                    classes.add(classDef)
                }
            }
        }

        if (type == PackageInfo.Type.APK) {
            val zipFile = ZipFile(inputPackageFile)
            val entries = zipFile.entries()

            // In the case of APKs with multiple dex files, ensure all dex files are loaded
            while (entries.hasMoreElements()) {
                val zipEntry = entries.nextElement()
                if (!zipEntry.isDirectory) {
                    val extension = FilenameUtils.getExtension(zipEntry.name).toLowerCase()
                    if (arrayOf("dex", "odex").contains(extension)) {
                        addClassesFromDex(zipFile.getInputStream(zipEntry), extension)
                    }
                }
            }
            zipFile.close()
        }

        if (type == PackageInfo.Type.DEX) {
            addClassesFromDex(inputPackageFile.inputStream(), inputPackageFile.extension)
        }

        Timber.d("Output directory: $workingDirectory")
        setStep(context.getString(R.string.mergingClasses))
        Timber.d("Total class to write ${classes.size}")
        setStep(context.getString(R.string.writingDexFile))

        val chunkedClasses = classes.chunked(
            data.getInt("chunkSize", UserPreferences.DEFAULTS.CHUNK_SIZE)
        )

        chunkedClasses.forEachIndexed { index, list ->
            Timber.d("Chunk $index with classes: ${list.size}")
            sendStatus(context.getString(R.string.chunk, index + 1, chunkedClasses.size), true)
            val dexFile = ImmutableDexFile(Opcodes.getDefault(), list)
            DexFileFactory.writeDexFile(
                outputDexFiles.resolve("$index.dex").canonicalPath,
                dexFile
            )
            Timber.d("DEX file location: ${this.outputDexFiles}")
        }
    }

    @Throws(Exception::class)
    private fun convertJarToDex() {
        cleanMemory()

        xyz.codezero.android.dx.command.dexer.Main.main(
            arrayOf(
                "--output",
                outputDexFiles.resolve("0.dex").canonicalPath,
                inputPackageFile.canonicalPath
            )
        )
    }

    /**
     * Convert the dex file to jar for CFR or Fernflower to use.
     * JaDX can directly make use of the dex file.
     */
    @Throws(Exception::class)
    private fun convertDexToJar() {
        cleanMemory()

        setStep(context.getString(R.string.startingDexToJar))

        val reuseReg = false // reuse register while generate java .class file
        val topologicalSort1 = false // same with --topological-sort/-ts
        val topologicalSort = false // sort block by topological, results in more readable code
        val verbose = true // show progress
        val debugInfo = false // translate debug info
        val printIR = false // print ir to System.out
        val optimizeSynchronized = true // Optimise-synchronised

        if (outputDexFiles.exists() && outputDexFiles.isDirectory) {

            setStep(context.getString(R.string.writingJarFile))

            outputDexFiles.listFiles().forEachIndexed { index, outputDexFile ->
                if (outputDexFile.exists() && outputDexFile.isFile) {
                    val dexExceptionHandlerMod = DexExceptionHandlerMod()
                    val reader = DexFileReader(outputDexFile)
                    val dex2jar = Dex2jar.from(reader)
                        .reUseReg(reuseReg)
                        .topoLogicalSort(topologicalSort || topologicalSort1)
                        .skipDebug(!debugInfo)
                        .optimizeSynchronized(optimizeSynchronized)
                        .printIR(printIR)
                        .verbose(verbose)
                    dex2jar.exceptionHandler = dexExceptionHandlerMod
                    dex2jar.to(outputJarFiles.resolve("$index.jar"))
                    outputDexFile.delete()
                }
            }
        }

    }

    /**
     * Handle dex2jar exception gracefully without causing a full crash
     */
    private inner class DexExceptionHandlerMod : DexExceptionHandler {
        override fun handleFileException(e: Exception) {
            Timber.d("Dex2Jar Exception $e")
        }

        override fun handleMethodTranslateException(
            method: Method,
            irMethod: IrMethod,
            methodNode: MethodNode,
            e: Exception
        ) {
            Timber.d("Dex2Jar Exception $e")
        }
    }

    override fun doWork(): ListenableWorker.Result {
        Timber.tag("JarExtraction")
        context.getString(R.string.extractingJar).let {
            buildNotification(it)
            setStep(it)
        }

        super.doWork()

        outputDexFiles.mkdirs()
        outputJarFiles.mkdirs()

        try {
            when (type) {
                PackageInfo.Type.APK, PackageInfo.Type.DEX -> {
                    loadIgnoredLibs()
                    convertApkToDex()
                    if (decompiler != "jadx") {
                        convertDexToJar()
                    }
                }
                PackageInfo.Type.JAR -> {
                    if (decompiler == "jadx") {
                        convertJarToDex()
                    } else {
                        inputPackageFile.copyTo(
                            outputJarFiles.resolve("0.jar"),
                            true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            return exit(e)
        }

        return successIf(
            try {
                outputJarFiles.listFiles().isNotEmpty() || outputDexFiles.listFiles().isNotEmpty()
            } catch (e: IllegalStateException) {
                false
            }
        )
    }
}
