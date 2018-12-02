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

package com.njlabs.showjava.test.decompilers.apk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.njlabs.showjava.test.DecompilerTestBase
import com.njlabs.showjava.data.PackageInfo
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApkViaCfr: DecompilerTestBase() {
    override val type: PackageInfo.Type = PackageInfo.Type.APK
    override val decompiler: String = "cfr"
}