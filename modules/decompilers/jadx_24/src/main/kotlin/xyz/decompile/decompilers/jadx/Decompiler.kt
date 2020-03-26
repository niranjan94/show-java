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

package xyz.decompile.decompilers.jadx

import com.njlabs.showjava.extractors.decompilers.BaseDecompiler
import com.njlabs.showjava.extractors.decompilers.DecompilerType
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import java.io.File
import java.io.PrintStream


class Decompiler(decompilerType: DecompilerType, printStream: PrintStream) : BaseDecompiler(decompilerType, printStream) {
    override fun extractResources(inputFiles: List<File>, outputDirectory: File) {
        val args = JadxArgs()
        args.outDirRes = outputDirectory
        args.inputFiles = inputFiles
        val jadx = JadxDecompiler(args)
        jadx.load()
        jadx.saveResources()
    }

    override fun extractSources(inputFiles: List<File>, outputDirectory: File) {
        val args = JadxArgs()
        args.outDirSrc = outputDirectory
        args.inputFiles = inputFiles.toMutableList()
        args.threadsCount = 1

        val jadx = JadxDecompiler(args)
        jadx.load()
        jadx.saveSources()
    }
}