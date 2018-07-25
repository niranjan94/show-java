package com.njlabs.showjava.services.processor.handlers

import com.googlecode.dex2jar.Method
import com.googlecode.dex2jar.ir.IrMethod
import com.googlecode.dex2jar.reader.DexFileReader
import com.googlecode.dex2jar.v3.Dex2jar
import com.googlecode.dex2jar.v3.DexExceptionHandler
import com.njlabs.showjava.services.processor.ProcessorService
import com.njlabs.showjava.utils.StringTools
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.immutable.ImmutableDexFile
import org.objectweb.asm.tree.MethodNode
import timber.log.Timber
import java.io.File
import java.io.PrintStream


class JarExtractor(private val processorService: ProcessorService) : BaseHandler(processorService) {

    private var ignoredLibs: ArrayList<String>? = ArrayList()
    private val perAppWorkingDirectory: File = File(processorService.sourceOutputDirectory)

    private var dexFile: File = File(perAppWorkingDirectory, "optimised_classes.dex")
    private var jarFile: File =
        File(perAppWorkingDirectory, "${processorService.inputPackageName}.jar")

    init {
        printStream = PrintStream(ProgressStream())
        System.setErr(printStream)
        System.setOut(printStream)
        Timber.tag("JarExtractor")
    }

    fun extract() {
        loadIgnoredLibs()
        convertApkToDex()
        if (processorService.decompilerToUse != "jadx") {
            convertDexToJar()
        }
    }

    private fun loadIgnoredLibs() {
        val ignoredList =
            if (processorService.shouldIgnoreLibs) "ignored.list" else "ignored_basic.list"
        processorService.application.assets.open(ignoredList).bufferedReader().useLines {
            it.map { line -> ignoredLibs?.add(StringTools.toClassName(line)) }
        }
    }

    private fun convertApkToDex() {

        Timber.i("Starting APK to DEX Conversion")

        var dexFile: DexFile? = null
        try {
            dexFile = DexFileFactory.loadDexFile("", Opcodes.getDefault())
        } catch (e: Exception) {
            broadcastStatus("exit")
        }

        val classes = ArrayList<ClassDef>()
        broadcastStatus("optimising")

        for (classDef in dexFile!!.classes) {
            if (!isIgnored(classDef.type)) {
                val currentClass = classDef.type
                broadcastStatus(
                    "optimising_class",
                    currentClass.replace("Processing ".toRegex(), "")
                )
                classes.add(classDef)
            }
        }

        broadcastStatus("optimise_dex_finish")
        Timber.i("Output directory: $perAppWorkingDirectory")
        broadcastStatus("merging_classes")
        dexFile = ImmutableDexFile(Opcodes.getDefault(), classes)

        try {
            this.dexFile = File(perAppWorkingDirectory, "optimised_classes.dex")
            DexFileFactory.writeDexFile(
                this.dexFile.absolutePath,
                dexFile
            )
            Timber.i("DEX file location: ${this.dexFile}")
        } catch (e: Exception) {
            Timber.e(e)
            broadcastStatus("exit", "cannot_decompile")
        }

    }

    private fun convertDexToJar() {
        Timber.i("Starting DEX to JAR Conversion")
        broadcastStatus("dex2jar")

        val reuseReg = false // reuse register while generate java .class file
        val topologicalSort1 = false // same with --topological-sort/-ts
        val topologicalSort =
            false // sort block by topological, that will generate more readable code
        val verbose = true // show progress
        val debugInfo = false // translate debug info
        val printIR = false // print ir to System.out
        val optimizeSynchronized = true // Optimise-synchronised

        if (dexFile.exists() && dexFile.isFile) {
            val dexExceptionHandlerMod = DexExceptionHandlerMod()
            try {
                val reader = DexFileReader(dexFile)
                val dex2jar = Dex2jar.from(reader).reUseReg(reuseReg)
                    .topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
                    .optimizeSynchronized(optimizeSynchronized).printIR(printIR).verbose(verbose)
                dex2jar.exceptionHandler = dexExceptionHandlerMod
                dex2jar.to(jarFile)
            } catch (e: Exception) {
                broadcastStatus("exit_process_on_error")
            }
            Timber.i("Clearing cache")
            dexFile.delete()
        }
    }


    private fun isIgnored(className: String): Boolean {
        return ignoredLibs!!.any { className.startsWith(it) }
    }

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

}