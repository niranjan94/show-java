import android.content.Context
import androidx.work.WorkerParameters
import com.googlecode.dex2jar.Method
import com.googlecode.dex2jar.ir.IrMethod
import com.googlecode.dex2jar.reader.DexFileReader
import com.googlecode.dex2jar.v3.Dex2jar
import com.googlecode.dex2jar.v3.DexExceptionHandler
import com.njlabs.showjava.utils.StringTools
import com.njlabs.showjava.workers.decompiler.BaseWorker
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.immutable.ImmutableDexFile
import org.objectweb.asm.tree.MethodNode
import timber.log.Timber

class JarExtractionWorker(context: Context, params: WorkerParameters) : BaseWorker(context, params) {

    private var ignoredLibs: ArrayList<String>? = ArrayList()

    private fun loadIgnoredLibs() {
        val ignoredList =
            if (data.getBoolean("shouldIgnoreLibs", false)) "ignored.list" else "ignored_basic.list"
        context.assets.open(ignoredList).bufferedReader().useLines {
            it.map { line -> ignoredLibs?.add(StringTools.toClassName(line)) }
        }
    }

    private fun convertApkToDex() {

        Timber.i("Starting APK to DEX Conversion")

        var dexFile: DexFile? = null
        try {
            dexFile = DexFileFactory.loadDexFile("", Opcodes.getDefault())
        } catch (e: Exception) {
            sendStatus("exit")
        }

        val classes = ArrayList<ClassDef>()
        sendStatus("optimising")

        for (classDef in dexFile!!.classes) {
            if (!isIgnored(classDef.type)) {
                val currentClass = classDef.type
                sendStatus(
                    "optimising_class",
                    currentClass.replace("Processing ".toRegex(), "")
                )
                classes.add(classDef)
            }
        }

        sendStatus("optimise_dex_finish")
        Timber.i("Output directory: $workingDirectory")
        sendStatus("merging_classes")
        dexFile = ImmutableDexFile(Opcodes.getDefault(), classes)

        try {
            DexFileFactory.writeDexFile(
                this.outputDexFile.canonicalPath,
                dexFile
            )
            Timber.i("DEX file location: ${this.outputDexFile}")
        } catch (e: Exception) {
            Timber.e(e)
            sendStatus("exit", "cannot_decompile")
        }

    }

    private fun convertDexToJar() {
        Timber.i("Starting DEX to JAR Conversion")
        sendStatus("dex2jar")

        val reuseReg = false // reuse register while generate java .class file
        val topologicalSort1 = false // same with --topological-sort/-ts
        val topologicalSort =
            false // sort block by topological, that will generate more readable code
        val verbose = true // show progress
        val debugInfo = false // translate debug info
        val printIR = false // print ir to System.out
        val optimizeSynchronized = true // Optimise-synchronised

        if (outputDexFile.exists() && outputDexFile.isFile) {
            val dexExceptionHandlerMod = DexExceptionHandlerMod()
            try {
                val reader = DexFileReader(outputDexFile)
                val dex2jar = Dex2jar.from(reader).reUseReg(reuseReg)
                    .topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
                    .optimizeSynchronized(optimizeSynchronized).printIR(printIR).verbose(verbose)
                dex2jar.exceptionHandler = dexExceptionHandlerMod
                dex2jar.to(outputJarFile)
            } catch (e: Exception) {
                sendStatus("exit_process_on_error")
            }
            Timber.i("Clearing cache")
            outputDexFile.delete()
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
    override fun doWork(): Result {
        Timber.tag("JarExtraction")
        super.doWork()
        loadIgnoredLibs()
        convertApkToDex()
        if (decompiler != "jadx") {
            convertDexToJar()
        }
        return Result.SUCCESS
    }
}
