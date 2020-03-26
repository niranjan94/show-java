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

package xyz.decompile.decompilers.fernflower

import com.njlabs.showjava.extractors.decompilers.BaseDecompiler
import com.njlabs.showjava.extractors.decompilers.DecompilerType
import com.njlabs.showjava.utils.ZipUtils
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream

class Decompiler(decompilerType: DecompilerType, printStream: PrintStream) : BaseDecompiler(decompilerType, printStream) {

    override fun extractResources(inputFiles: List<File>, outputDirectory: File) {
        TODO("Not yet implemented")
    }

    override fun extractSources(inputFiles: List<File>, outputDirectory: File) {
        ConsoleDecompiler.main(
            arrayOf(
                inputFiles.first().canonicalPath, outputDirectory.canonicalPath
            )
        )

        outputDirectory.listFiles()?.forEach { decompiledJarFile ->
            if (decompiledJarFile.exists() && decompiledJarFile.isFile && decompiledJarFile.extension == "jar") {
                ZipUtils.unzip(decompiledJarFile, outputDirectory, printStream)
                decompiledJarFile.delete()
            } else {
                throw FileNotFoundException("Decompiled jar does not exist")
            }
        }
    }
}