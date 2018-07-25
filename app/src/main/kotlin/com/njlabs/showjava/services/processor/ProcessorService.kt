package com.njlabs.showjava.services.processor

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import com.njlabs.showjava.Constants
import com.njlabs.showjava.activities.decompiler.DecompilerActivity
import com.njlabs.showjava.services.processor.handlers.JarExtractor
import com.njlabs.showjava.utils.ExceptionHandler
import com.njlabs.showjava.utils.PackageSourceTools
import net.dongliu.apk.parser.ApkFile
import java.io.File

class ProcessorService : BaseProcessorService() {

    var shouldIgnoreLibs: Boolean = false

    private lateinit var uiHandler: Handler

    private var stackSize: Long = 20 * 1024 * 1024
    private var shouldIgnoreLibraries: Boolean = true

    var decompilerToUse: String = "jadx"

    lateinit var inputPackageFilePath: String
    lateinit var inputPackageName: String
    lateinit var inputPackageLabel: String
    lateinit var parsedInputApkFile: ApkFile

    var sourceOutputDirectory: String = ""
    var javaSourceOutputDirectory: String = ""

    lateinit var exceptionHandler: ExceptionHandler

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        this.startID = startId
        uiHandler = Handler()
        when {
            intent.action == Constants.ACTION.START_PROCESS -> {
                startForeground(Constants.PROCESS_NOTIFICATION_ID, buildNotification())
                handleIntent(intent)
            }
            intent.action == Constants.ACTION.STOP_PROCESS -> {
                val toKillStartId = intent.getIntExtra("startId", -1)
                killSelf(true, toKillStartId)
            }
            intent.action == Constants.ACTION.STOP_PROCESS_FOR_NEW -> killSelf(false, -1)
        }

        return Service.START_NOT_STICKY
    }


    private fun handleIntent(workIntent: Intent) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        stackSize = Integer.valueOf(
            prefs.getString(
                "thread_stack_size",
                (20 * 1024 * 1024).toString()
            )
        ).toLong()
        shouldIgnoreLibraries = prefs.getBoolean("ignore_libraries", true)

        val extras = workIntent.extras
        if (extras != null) {

            if (extras.containsKey("decompiler")) {
                decompilerToUse = extras.getString("decompiler")
            }

            inputPackageFilePath = extras.getString("package_file_path")
            Thread(Runnable {
                try {
                    val packageInfo = packageManager.getPackageArchiveInfo(inputPackageFilePath, 0)
                    parsedInputApkFile = ApkFile(File(inputPackageFilePath))
                    inputPackageLabel = parsedInputApkFile.apkMeta.label
                    inputPackageName = packageInfo.packageName
                } catch (e: Exception) {
                    broadcastStatus("exit_process_on_error")
                }

                exceptionHandler = ExceptionHandler(
                    applicationContext,
                    sourceOutputDirectory,
                    inputPackageName
                )

                uiHandler.post {
                    val resultIntent = Intent(applicationContext, DecompilerActivity::class.java)
                    resultIntent.putExtra("from_notification", true)
                    resultIntent.putExtra("package_name", packageName)
                    resultIntent.putExtra("package_label", inputPackageLabel)
                    resultIntent.putExtra("package_file_path", inputPackageFilePath)
                    resultIntent.putExtra("decompiler", decompilerToUse)

                    val resultPendingIntent = PendingIntent.getActivity(
                        this@ProcessorService,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    processNotifier?.updateIntent(resultPendingIntent)
                }

                sourceOutputDirectory =
                        "${Environment.getExternalStorageState()}/ShowJava/sources/$packageName"
                javaSourceOutputDirectory = "$sourceOutputDirectory/java"

                PackageSourceTools.initialise(
                    inputPackageLabel,
                    inputPackageName,
                    sourceOutputDirectory
                )

                uiHandler.post {
                    Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
                }
            }).start()
        } else {
            killSelf(true, startID)
        }
    }

    private fun startExtraction() {
        val group = ThreadGroup("$inputPackageName-extractor-thread-group")
        val runProcess = Runnable {
            val jarExtractor = JarExtractor(this@ProcessorService)
            jarExtractor.extract()
        }
        val extractionThread =
            Thread(group, runProcess, "$inputPackageName-extractor-thread", stackSize)
        extractionThread.priority = Thread.MAX_PRIORITY
        extractionThread.uncaughtExceptionHandler = exceptionHandler
        extractionThread.start()
    }

}
