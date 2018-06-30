package com.njlabs.showjava.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter


class ExceptionHandler(private val context: Context, private val sourceDir: String, private val packageID: String ) : java.lang.Thread.UncaughtExceptionHandler {

    private val LINE_SEPARATOR = "\n"

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        val errorReport = StringBuilder()
        errorReport.append("************ CAUSE OF ERROR ************\n\n")
        errorReport.append(stackTrace.toString())

        Timber.e(exception)

        Toast.makeText(
            context,
            "There was an error decompiling this app. Showing incomplete source.",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(context, NavigatorActivity::class.java)
        intent.putExtra("java_source_dir", sourceDir)
        intent.putExtra("package_id", packageID)
        context.startActivity(intent)

        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(10)
    }

}
