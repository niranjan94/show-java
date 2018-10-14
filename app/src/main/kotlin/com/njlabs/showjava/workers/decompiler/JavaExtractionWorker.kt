import android.content.Context
import androidx.work.WorkerParameters
import com.njlabs.showjava.workers.decompiler.BaseWorker
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

class JavaExtractionWorker(context : Context, params : WorkerParameters) : BaseWorker(context, params) {

    private fun decompileWithCFR(jarInputFile: File, javaOutputDir: File): Boolean {
        val args = arrayOf(jarInputFile.toString(), "--outputdir", javaOutputDir.toString())
        val getOptParser = GetOptParser()
        var javaExtractionError = true
        val options: Options?
        try {
            options = getOptParser.parse<Options>(args, OptionsImpl.getFactory())

            if (!options!!.optionIsSet(OptionsImpl.HELP) && options.getOption(OptionsImpl.FILENAME) != null) {
                val classFileSource = ClassFileSourceImpl(options)
                val dcCommonState = DCCommonState(options, classFileSource)
                val path = options.getOption(OptionsImpl.FILENAME)
                val dumperFactory = DumperFactoryImpl()
                try {
                    Main.doJar(dcCommonState, path, dumperFactory)
                } catch (e: Exception) {
                    Timber.e(e)
                    javaExtractionError = true
                } catch (e: StackOverflowError) {
                    Timber.e(e)
                    javaExtractionError = true
                }
            } else {
                sendStatus("exit_process_on_error")
            }

        } catch (e: Exception) {
            Timber.e(e)
            sendStatus("exit_process_on_error")
        }

        return javaExtractionError
    }

    private fun decompileWithJaDX(dexInputFile: File, javaOutputDir: File): Boolean {
        var javaExtractionError = true
        try {
            val jadx = JadxDecompiler()
            jadx.setOutputDir(javaOutputDir)
            jadx.loadFile(dexInputFile)
            jadx.saveSources()
        } catch (e: Exception) {
            Timber.e(e)
            javaExtractionError = true
        } catch (e: StackOverflowError) {
            Timber.e(e)
            javaExtractionError = true
        }

        if (dexInputFile.exists() && dexInputFile.isFile) {
            dexInputFile.delete()
        }
        return javaExtractionError
    }

    private fun decompileWithFernFlower(jarInputFile: File, javaOutputDir: File): Boolean {
        var javaExtractionError = true
        try {

            val mapOptions = HashMap<String, Any>()
            val logger = PrintStreamLogger(printStream)
            val decompiler = ConsoleDecompiler(javaOutputDir, mapOptions, logger)
            decompiler.addSpace(jarInputFile, true)
            decompiler.decompileContext()

            if (outputJarFile.exists()) {
                ZipUtils.unzip(outputJarFile, javaOutputDir, printStream!!)
                outputJarFile.delete()
            } else {
                javaExtractionError = true
            }

        } catch (e: Exception) {
            Timber.e(e)
            javaExtractionError = true
        } catch (e: StackOverflowError) {
            Timber.e(e)
            javaExtractionError = true
        }

        return javaExtractionError
    }

    override fun doWork(): Result {
        Timber.tag("JavaExtraction")
        super.doWork()

        sendStatus("jar2java")

        if (decompiler != "jadx") {
            if (outputDexFile.exists() && outputDexFile.isFile) {
                outputDexFile.delete()
            }
        }

        val javaExtractionError: Boolean = when (decompiler) {
            "jadx" -> decompileWithJaDX(outputDexFile, outputJavaSrcDirectory)
            "cfr" -> decompileWithCFR(outputJarFile, outputJavaSrcDirectory)
            "fernflower" -> decompileWithFernFlower(outputJarFile, outputJavaSrcDirectory)
            else -> false
        }

        PackageSourceTools.setJavaSourceStatus(workingDirectory.canonicalPath, javaExtractionError)

        return Result.SUCCESS
    }
}
