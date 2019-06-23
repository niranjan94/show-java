/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2019 Niranjan Rajendran
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

package com.njlabs.showjava.utils.streams

import jadx.api.JadxArgs
import jadx.core.dex.nodes.ClassNode
import java.io.File


object Logger {

    /**
     * This method will be invoked by a JaDX method that is used to save classes.
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun logJadxClassWrite(dir: File, args: JadxArgs, cls: ClassNode) {
        println("Decompiling " + cls.fullName)
    }
}
