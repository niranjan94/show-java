package com.njlabs.showjava.services.processor.handlers

import android.support.annotation.NonNull
import android.widget.Toast
import com.njlabs.showjava.services.processor.ProcessorService
import timber.log.Timber
import java.io.OutputStream
import java.io.PrintStream


open class BaseHandler(private val processorService: ProcessorService) {
    var printStream: PrintStream? = null

    fun broadcastStatus(status: String) {
        processorService.broadcastStatus(status)
    }

    fun broadcastStatus(statusKey: String, statusData: String) {
        processorService.broadcastStatus(statusKey, statusData)
    }

    fun broadcastStatusWithPackageInfo(statusKey: String, dir: String, packId: String) {
        processorService.broadcastStatusWithPackageInfo(statusKey, dir, packId)
    }

    /**
     * A toast fn in the form of a runnable so that it can be executed in the UI thread
     */
    protected inner class ToastRunnable(private var mText: String) : Runnable {
        override fun run() {
            Toast.makeText(processorService.applicationContext, mText, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * A custom output stream that strips unnecessary stuff from raw input stream
     */
    inner class ProgressStream : OutputStream() {
        override fun write(@NonNull data: ByteArray, offset: Int, length: Int) {
            val str = String(data)
                .replace("\n", "")
                .replace("\r", "")
                .replace("INFO:", "")
                .replace("ERROR:", "")
                .replace("WARN:", "")
                .replace("\n\r", "")
                .replace("... done", "")
                .replace("at", "")
                .trim()
            if (str.isNotEmpty()) {
                Timber.i("[ProgressStream] $str")
                broadcastStatus("progress_stream", str)
            }
        }

        override fun write(byte: Int) {
            // Just a stub. We aren't implementing this.
        }
    }

}