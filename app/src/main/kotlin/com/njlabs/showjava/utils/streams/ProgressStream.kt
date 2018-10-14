package com.njlabs.showjava.utils.streams

import androidx.annotation.NonNull
import timber.log.Timber
import java.io.OutputStream

/**
 * A custom output stream that strips unnecessary stuff from raw input stream
 */
class ProgressStream : OutputStream() {
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
        }
    }
    override fun write(byte: Int) {
        // Just a stub. We aren't implementing this.
    }
}