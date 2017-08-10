package com.njlabs.showjava.utils

object StringTools {
    fun replace(text: String, searchString: String, replacement: String): String {
        return replace(text, searchString, replacement, -1)
    }

    fun replace(text: String, searchString: String, replacement: String?, _max: Int): String {
        var max = _max
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
            return text
        }
        var start = 0
        var end = text.indexOf(searchString, start)
        if (end == -1) {
            return text
        }
        val replLength = searchString.length
        var increase = replacement.length - replLength
        increase = if (increase < 0) 0 else increase
        increase *= if (max < 0) 16 else if (max > 64) 64 else max
        val buf = StringBuffer(text.length + increase)
        while (end != -1) {
            buf.append(text.substring(start, end)).append(replacement)
            start = end + replLength
            if (--max == 0) {
                break
            }
            end = text.indexOf(searchString, start)
        }
        buf.append(text.substring(start))
        return buf.toString()
    }

    fun isEmpty(testString: String): Boolean {
        return "" == testString
    }

    fun toClassName(packageName: String): String {
        return "L" + replace(packageName.trim { it <= ' ' }, ".", "/")
    }

    fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return bytes.toString() + " B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
}
