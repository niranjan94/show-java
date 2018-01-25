package com.njlabs.showjava.activities.filepicker

import com.nononsenseapps.filepicker.AbstractFilePickerFragment
import java.io.File

class FilePickerFragment : com.nononsenseapps.filepicker.FilePickerFragment() {

    private val extensions = arrayOf(".apk", ".jar")

    /**
     * @param file input file
     * *
     * @return The file extension. If file has no extension, it returns null.
     */
    private fun getExtension(file: File): String? {
        val path = file.path
        val i = path.lastIndexOf(".")
        return if (i < 0) {
            null
        } else {
            path.substring(i)
        }
    }

    /**
     * Allow only selected extensions.
     */
    override fun isItemVisible(file: File?): Boolean {
        if (file != null) {
            if (!isDir(file) && (mode == MODE_FILE || mode == MODE_FILE_AND_DIR)) {
                return extensions.contains(getExtension(file)?.toLowerCase())
            }
            return isDir(file)
        }
        return false
    }

    /**
     * For consistency, the top level the back button checks against should be the start path.
     * But it will fall back on /.
     */
    private val backTop: File
        get() {
            return if (arguments!!.containsKey(AbstractFilePickerFragment.KEY_START_PATH)) {
                val keyStartPath = arguments!!.getString(AbstractFilePickerFragment.KEY_START_PATH)
                getPath(keyStartPath ?: "/")
            } else {
                File("/")
            }
        }

    /**
     * @return true if the current path is the start path or /
     */
    val isBackTop: Boolean
        get() = 0 == compareFiles(mCurrentPath, backTop) || 0 == compareFiles(mCurrentPath, File("/"))

    /**
     * Go up on level, same as pressing on "..".
     */
    override fun goUp() {
        mCurrentPath = getParent(mCurrentPath)
        mCheckedItems.clear()
        mCheckedVisibleViewHolders.clear()
        refresh(mCurrentPath)
    }
}
