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