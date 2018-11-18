package com.njlabs.showjava.activities.decompiler

import android.os.Bundle
import androidx.work.WorkManager
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.decompilers.BaseDecompiler
import kotlinx.android.synthetic.main.activity_decompiler.*


class DecompilerActivity : BaseActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_decompiler)
        val packageFilePath = intent.getStringExtra("packageFilePath")
        val packageInfo = packageManager.getPackageArchiveInfo(packageFilePath, 0)

        itemIcon.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager))
        itemLabel.text = packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
        itemSecondaryLabel.text = if (packageInfo.versionName != null)
            packageInfo.versionName else packageInfo.versionCode.toString()

        if (intent.action == Constants.ACTION.STOP_PROCESS) {
            WorkManager.getInstance().cancelAllWorkByTag(intent.getStringExtra("id"))
        }

    }

    fun startProcess() {
        BaseDecompiler.start(hashMapOf(
            "shouldIgnoreLibs" to true,
            "decompiler" to "cfr",
            "name" to "",
            "label" to "",
            "inputPackageFile" to ""
        ))
    }
}