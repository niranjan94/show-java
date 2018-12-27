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

package com.njlabs.showjava.test

import android.os.Build
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import com.njlabs.showjava.data.PackageInfo
import com.njlabs.showjava.decompilers.BaseDecompiler
import com.njlabs.showjava.decompilers.JarExtractionWorker
import com.njlabs.showjava.decompilers.JavaExtractionWorker
import com.njlabs.showjava.decompilers.ResourcesExtractionWorker
import junit.framework.TestCase
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

abstract class DecompilerTestBase {

    abstract val decompiler: String
    abstract val type: PackageInfo.Type

    private val testAssets: File
        get() = File(Environment.getExternalStorageDirectory(), "show-java")
            .resolve("test-assets")

    private val testApplicationFile: File
        get() = testAssets.resolve("test-application.${type.name.toLowerCase()}")

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
        if (testAssets.exists() && testAssets.isFile) {
            testAssets.delete()
        }
        if (!testAssets.exists()) {
            testAssets.mkdirs()
        }
        if (!testApplicationFile.exists()) {
            testApplicationFile.outputStream().use {
                appContext.context.assets
                    .open("test-application.${type.name.toLowerCase()}")
                    .copyTo(it)
            }
        }
    }

    @Before
    fun checkDecompilerAvailability() {
        Assume.assumeTrue(
            "Assume $decompiler is available on API ${Build.VERSION.SDK_INT}.",
            BaseDecompiler.isAvailable(decompiler)
        )
    }

    @Test
    fun testDecompiler() {
        val data = BaseDecompiler.formData(hashMapOf(
            "shouldIgnoreLibs" to true,
            "keepIntermediateFiles" to true,
            "chunkSize" to 2000,
            "maxAttempts" to 1,
            "memoryThreshold" to 80,
            "decompiler" to decompiler,
            "name" to "xyz.codezero.testapplication.$decompiler.${type.name}",
            "label" to "TestApplication-$decompiler-${type.name}",
            "inputPackageFile" to testApplicationFile.canonicalPath,
            "type" to type.ordinal
        ))

        val packageInfo = PackageInfo.fromFile(
            InstrumentationRegistry.getInstrumentation().targetContext,
            testApplicationFile
        )

        TestCase.assertNotNull("Can parse PackageInfo from file", packageInfo)

        val outputDirectory = File(
            Environment.getExternalStorageDirectory(), "show-java/sources/${data.getString("name")}"
        )

        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }

        val appContext = InstrumentationRegistry.getInstrumentation()

        var result: ListenableWorker.Result
        var worker: BaseDecompiler

        worker = JarExtractionWorker(appContext.targetContext, data)
        result = worker.doWork()
        worker.onStopped()
        TestCase.assertEquals("Can extract JAR", ListenableWorker.Result.SUCCESS, result)

        worker = JavaExtractionWorker(appContext.targetContext, data)
        result = worker.doWork()
        worker.onStopped()
        TestCase.assertEquals("Can extract JAVA Code", ListenableWorker.Result.SUCCESS, result)

        worker = ResourcesExtractionWorker(appContext.targetContext, data)
        result = worker.doWork()
        worker.onStopped()
        TestCase.assertEquals("Can extract resources", ListenableWorker.Result.SUCCESS, result)
    }
}