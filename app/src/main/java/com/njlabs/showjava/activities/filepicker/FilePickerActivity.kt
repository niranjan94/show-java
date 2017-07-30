package com.njlabs.showjava.activities.filepicker

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem

import com.nononsenseapps.filepicker.AbstractFilePickerActivity
import com.nononsenseapps.filepicker.AbstractFilePickerFragment

import java.io.File

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

class FilePickerActivity : AbstractFilePickerActivity<File>() {

    private var currentFragment: FilePickerFragment? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }
    }

    override fun getFragment(startPath: String?,
                             mode: Int,
                             allowMultiple: Boolean,
                             allowCreateDir: Boolean,
                             allowExistingFile: Boolean,
                             singleClick: Boolean): AbstractFilePickerFragment<File> {
        currentFragment = FilePickerFragment()
        currentFragment?.setArgs(
                startPath ?: Environment.getExternalStorageDirectory().path,
                mode,
                allowMultiple,
                allowCreateDir,
                allowExistingFile,
                singleClick
        )
        return currentFragment as FilePickerFragment
    }

    /**
     * Override the back-button.
     */
    override fun onBackPressed() {
        // If at top most level, normal behaviour
        if (currentFragment == null || currentFragment!!.isBackTop) {
            finish()
        } else {
            // Else go up
            currentFragment!!.goUp()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
