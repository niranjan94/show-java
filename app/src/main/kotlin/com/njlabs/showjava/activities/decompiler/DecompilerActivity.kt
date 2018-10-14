package com.njlabs.showjava.activities.decompiler

import android.os.Bundle
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.data.PackageInfo

class DecompilerActivity : BaseActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_decompiler)
        val packageInfo = intent.getParcelableExtra<PackageInfo>("packageInfo")
        val decompiler = intent.getStringExtra("decompiler")
        println(packageInfo)
        println(decompiler)
    }

}