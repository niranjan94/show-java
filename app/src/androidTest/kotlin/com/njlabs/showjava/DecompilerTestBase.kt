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
 * along with this program.  If not, gsee <https://www.gnu.org/licenses/>.
 */

package com.njlabs.showjava

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import com.njlabs.showjava.decompilers.BaseDecompiler
import com.njlabs.showjava.decompilers.JarExtractionWorker
import com.njlabs.showjava.decompilers.JavaExtractionWorker
import com.njlabs.showjava.decompilers.ResourcesExtractionWorker
import junit.framework.TestCase
import org.junit.Before
import org.junit.Rule
import java.io.File

abstract class DecompilerTestBase {

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    @Before
    fun initializeEnvironment() {
        val appContext = InstrumentationRegistry.getInstrumentation()
        if (!testApplicationFile.exists()) {
            testApplicationFile.outputStream().use {
                appContext.context.assets.open("test-application.apk").copyTo(it)
            }
        }
    }

    private val testApplicationFile: File
        get() = File(Environment.getExternalStorageDirectory(), "test-application.apk")

    fun useDecompiler(name: String) {
        val data = BaseDecompiler.formData(hashMapOf(
            "shouldIgnoreLibs" to true,
            "decompiler" to name,
            "name" to "xyz.codezero.testapplication.$name",
            "label" to "TestApplication-$name",
            "inputPackageFile" to testApplicationFile.canonicalPath
        ))

        val outputDirectory = File(
            Environment.getExternalStorageDirectory(), "show-java/sources/${data.getString("name")}"
        )

        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }

        val appContext = InstrumentationRegistry.getInstrumentation()

        val jarExtractionWorker = JarExtractionWorker(appContext.targetContext, data)
        TestCase.assertEquals(ListenableWorker.Result.SUCCESS, jarExtractionWorker.doWork())

        val javaExtractionWorker = JavaExtractionWorker(appContext.targetContext, data)
        TestCase.assertEquals(ListenableWorker.Result.SUCCESS, javaExtractionWorker.doWork())

        val resourcesExtractionWorker = ResourcesExtractionWorker(appContext.targetContext, data)
        TestCase.assertEquals(ListenableWorker.Result.SUCCESS, resourcesExtractionWorker.doWork())
    }

    abstract fun runDecompiler()
}