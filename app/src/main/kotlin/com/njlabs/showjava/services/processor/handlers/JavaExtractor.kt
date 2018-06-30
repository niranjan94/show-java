package com.njlabs.showjava.services.processor.handlers

import com.njlabs.showjava.services.processor.ProcessorService
import com.njlabs.showjava.utils.PackageSourceTools
import com.njlabs.showjava.utils.ZipUtils
import jadx.api.JadxDecompiler
import org.benf.cfr.reader.Main
import org.benf.cfr.reader.state.ClassFileSourceImpl
import org.benf.cfr.reader.state.DCCommonState
import org.benf.cfr.reader.util.getopt.GetOptParser
import org.benf.cfr.reader.util.getopt.Options
import org.benf.cfr.reader.util.getopt.OptionsImpl
import org.benf.cfr.reader.util.output.DumperFactoryImpl
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger
import timber.log.Timber
import java.io.File
import java.io.PrintStream


class JavaExtractor(private val processorService: ProcessorService) : BaseHandler(processorService) {
    private var ignoredLibs: ArrayList<String>? = null

    private val sourceOutputDirectory: File = File(processorService.sourceOutputDirectory)
    private val javaSourceOutputDirectory: File = File(processorService.javaSourceOutputDirectory)

    private var dexFile: File = File(sourceOutputDirectory, "optimised_classes.dex")
    private var jarFile: File = File(javaSourceOutputDirectory, "${processorService.inputPackageName}.jar")

    init {
        ignoredLibs = ArrayList()
        printStream = PrintStream(ProgressStream())
        System.setErr(printStream)
        System.setOut(printStream)

        Timber.tag("JavaExtractor")
    }


    fun extract() {
        broadcastStatus("jar2java")

        if (!javaSourceOutputDirectory.isDirectory) {
            javaSourceOutputDirectory.mkdirs()
        }

        if (processorService.decompilerToUse != "jadx") {
            if (dexFile.exists() && dexFile.isFile) {
                dexFile.delete()
            }
        }

        when (processorService.decompilerToUse) {
            "jadx" -> decompileWithJaDX(dexFile, javaSourceOutputDirectory)
            "cfr" -> decompileWithCFR(jarFile, javaSourceOutputDirectory)
            "fernflower" -> decompileWithFernFlower(jarFile, javaSourceOutputDirectory)
        }
    }

    private fun decompileWithCFR(jarInputFile: File, javaOutputDir: File) {
        val args = arrayOf(jarInputFile.toString(), "--outputdir", javaOutputDir.toString())
        val getOptParser = GetOptParser()

        val options: Options?
        try {
            options = getOptParser.parse<Options>(args, OptionsImpl.getFactory())

            if (!options!!.optionIsSet(OptionsImpl.HELP) && options.getOption(OptionsImpl.FILENAME) != null) {
                val classFileSource = ClassFileSourceImpl(options)
                val dcCommonState = DCCommonState(options, classFileSource)
                val path = options.getOption(OptionsImpl.FILENAME)
                val dumperFactory = DumperFactoryImpl()
                var javaError = false
                try {
                    Main.doJar(dcCommonState, path, dumperFactory)
                } catch (e: Exception) {
                    Timber.e(e)
                    javaError = true
                } catch (e: StackOverflowError) {
                    Timber.e(e)
                    javaError = true
                }
                startXMLExtractor(!javaError)

            } else {
                broadcastStatus("exit_process_on_error")
            }

        } catch (e: Exception) {
            Timber.e(e)
            broadcastStatus("exit_process_on_error")
        }

    }

    private fun decompileWithJaDX(dexInputFile: File, javaOutputDir: File) {
        var javaError = false
        try {
            val jadx = JadxDecompiler()
            jadx.setOutputDir(javaOutputDir)
            jadx.loadFile(dexInputFile)
            jadx.saveSources()
        } catch (e: Exception) {
            Timber.e(e)
            javaError = true
        } catch (e: StackOverflowError) {
            Timber.e(e)
            javaError = true
        }

        if (dexInputFile.exists() && dexInputFile.isFile) {
            dexInputFile.delete()
        }
        startXMLExtractor(!javaError)
    }

    private fun decompileWithFernFlower(jarInputFile: File, javaOutputDir: File) {
        var javaError = false
        try {

            val mapOptions = HashMap<String, Any>()
            val logger = PrintStreamLogger(printStream)
            val decompiler = ConsoleDecompiler(javaOutputDir, mapOptions, logger)
            decompiler.addSpace(jarInputFile, true)
            decompiler.decompileContext()

            if (jarFile.exists()) {
                ZipUtils.unzip(jarFile, javaOutputDir, printStream!!)
                jarFile.delete()
            } else {
                javaError = true
            }

        } catch (e: Exception) {
            Timber.e(e)
            javaError = true
        } catch (e: StackOverflowError) {
            Timber.e(e)
            javaError = true
        }

        startXMLExtractor(!javaError)

    }

    private fun startXMLExtractor(hasJava: Boolean) {
        PackageSourceTools.setJavaSourceStatus(sourceOutputDirectory.absolutePath, hasJava)
    }
}