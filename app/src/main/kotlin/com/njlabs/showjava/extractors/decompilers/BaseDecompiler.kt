/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2020 Niranjan Rajendran
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

package com.njlabs.showjava.extractors.decompilers

import timber.log.Timber
import java.io.File
import java.io.PrintStream
import java.util.*

abstract class BaseDecompiler(private val decompilerType: DecompilerType, protected val printStream: PrintStream) {
    val name: String
        get() = decompilerType.name.toLowerCase(Locale.ENGLISH)

    @Throws(NotImplementedError::class, Exception::class)
    abstract fun extractResources(inputFiles: List<File>, outputDirectory: File)

    @Throws(Exception::class)
    abstract fun extractSources(inputFiles: List<File>, outputDirectory: File)
}


enum class DecompilerType { CFR, JADX, FERNFLOWER }

private val decompilers = mutableMapOf<DecompilerType,  Class<*>>()

fun getSupportedDecompilers(): MutableMap<DecompilerType, Class<*>> {
    if (decompilers.isNotEmpty()) {
        return decompilers
    }
    DecompilerType.values().forEach {
        val decompiler = it.name.toLowerCase(Locale.ENGLISH)
        val className = "xyz.decompile.decompilers.${decompiler}.Decompiler"
        try {
            decompilers[it] = Class.forName(className)
        } catch (e: ClassNotFoundException) {
            // Ignored if class is not found
            Timber.d("Could not find class `${className}` for decompiler ${it.name}")
        }
    }
    return decompilers
}

@Throws(ClassNotFoundException::class)
fun getDecompilerInstance(decompilerType: DecompilerType, printStream: PrintStream): BaseDecompiler {
    val supportedDecompilers = getSupportedDecompilers()
    if (!supportedDecompilers.contains(decompilerType)) {
        throw ClassNotFoundException()
    }

    val decompilerInstance = supportedDecompilers[decompilerType]?.constructors?.first()?.newInstance(decompilerType, printStream)
    if (decompilerInstance == null || decompilerInstance !is BaseDecompiler) {
        throw ClassNotFoundException()
    }

    return decompilerInstance
}