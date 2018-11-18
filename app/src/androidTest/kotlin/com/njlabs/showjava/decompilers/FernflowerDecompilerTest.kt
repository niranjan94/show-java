package com.njlabs.showjava.decompilers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.njlabs.showjava.DecompilerTestBase
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FernflowerDecompilerTest: DecompilerTestBase() {

    @Test
    override fun runDecompiler() {
        useDecompiler("fernflower")
    }
}