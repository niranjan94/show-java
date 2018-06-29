package com.njlabs.showjava.utils

import java.text.Normalizer
import java.util.*
import java.util.regex.Pattern

object StringTools {

    private val NON_LATIN = Pattern.compile("[^\\w-]")
    private val WHITESPACE = Pattern.compile("[\\s]")

    fun toClassName(packageName: String): String {
        return "L" + packageName.trim().replace(".", "/")
    }

    fun toSlug(input: String): String {
        val noWhiteSpace = WHITESPACE.matcher(input).replaceAll("-")
        val normalized = Normalizer.normalize(noWhiteSpace, Normalizer.Form.NFD)
        val slug = NON_LATIN.matcher(normalized).replaceAll("")
        return slug.toLowerCase(Locale.ENGLISH)
    }

    fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return bytes.toString() + " B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
}
