package com.njlabs.showjava.decompilers

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
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
import timber.log.Timber
import java.io.File

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
