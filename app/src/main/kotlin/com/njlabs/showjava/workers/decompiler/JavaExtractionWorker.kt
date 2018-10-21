package com.njlabs.showjava.workers.decompiler

import android.content.Context
import androidx.work.WorkerParameters
import com.njlabs.showjava.R
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
import java.lang.Exception

class JavaExtractionWorker(context : Context, params : WorkerParameters) : BaseWorker(context, params) {

    @Throws(Exception::class)
    private fun decompileWithCFR(jarInputFile: File, javaOutputDir: File) {
        val args = arrayOf(jarInputFile.toString(), "--outputdir", javaOutputDir.toString())
        val getOptParser = GetOptParser()
        val options: Options?
        options = getOptParser.parse<Options>(args, OptionsImpl.getFactory())

        if (!options!!.optionIsSet(OptionsImpl.HELP) && options.getOption(OptionsImpl.FILENAME) != null) {
            val classFileSource = ClassFileSourceImpl(options)
            val dcCommonState = DCCommonState(options, classFileSource)
            val path = options.getOption(OptionsImpl.FILENAME)
            val dumperFactory = DumperFactoryImpl()
            Main.doJar(dcCommonState, path, dumperFactory)
        } else {
            sendStatus("exit_process_on_error")
        }
    }

    @Throws(Exception::class)
    private fun decompileWithJaDX(dexInputFile: File, javaOutputDir: File) {
        val jadx = JadxDecompiler()
        jadx.setOutputDir(javaOutputDir)
        jadx.loadFile(dexInputFile)
        jadx.saveSources()

        if (dexInputFile.exists() && dexInputFile.isFile) {
            dexInputFile.delete()
        }
    }

    @Throws(Exception::class)
    private fun decompileWithFernFlower(jarInputFile: File, javaOutputDir: File) {
        val mapOptions = HashMap<String, Any>()
        val logger = PrintStreamLogger(printStream)
        val decompiler = ConsoleDecompiler(javaOutputDir, mapOptions, logger)
        decompiler.addSpace(jarInputFile, true)
        decompiler.decompileContext()

        if (outputJarFile.exists()) {
            ZipUtils.unzip(outputJarFile, javaOutputDir, printStream!!)
            outputJarFile.delete()
        } else {
            sendStatus("exit_process_on_error")
        }
    }

    override fun doWork(): Result {
        Timber.tag("JavaExtraction")
        buildNotification(context.getString(R.string.decompilingToJava))

        super.doWork()

        sendStatus("jar2java")

        if (decompiler != "jadx") {
            if (outputDexFile.exists() && outputDexFile.isFile) {
                outputDexFile.delete()
            }
        }

        try {
            when (decompiler) {
                "jadx" -> decompileWithJaDX(outputDexFile, outputJavaSrcDirectory)
                "cfr" -> decompileWithCFR(outputJarFile, outputJavaSrcDirectory)
                "fernflower" -> decompileWithFernFlower(outputJarFile, outputJavaSrcDirectory)
            }
        } catch (e: Exception) {
            PackageSourceTools.setJavaSourceStatus(workingDirectory.canonicalPath, true)
            return exit(e)
        }

        PackageSourceTools.setJavaSourceStatus(workingDirectory.canonicalPath, false)
        return Result.SUCCESS
    }
}
