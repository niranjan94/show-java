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

package com.njlabs.showjava.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter


class ExceptionHandler(
    private val context: Context,
    private val sourceDir: String,
    private val packageID: String
) : java.lang.Thread.UncaughtExceptionHandler {

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
