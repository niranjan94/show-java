package com.njlabs.showjava.ui;

import android.support.annotation.NonNull;

import java.io.File;

public class FilePickerFragment extends com.nononsenseapps.filepicker.FilePickerFragment {

    private static final String EXTENSION = ".apk";

    /**
     * @param file input file
     * @return The file extension. If file has no extension, it returns null.
     */
    private String getExtension(@NonNull File file) {
        String path = file.getPath();
        int i = path.lastIndexOf(".");
        if (i < 0) {
            return null;
        } else {
            return path.substring(i);
        }
    }

    @Override
    protected boolean isItemVisible(final File file) {
        // Default behavior
        // return isDir(file) || (mode == MODE_FILE || mode == MODE_FILE_AND_DIR);
        if (!isDir(file) && (mode == MODE_FILE || mode == MODE_FILE_AND_DIR)) {
            return EXTENSION.equalsIgnoreCase(getExtension(file));
        }
        return isDir(file);
    }

    /**
     * For consistency, the top level the back button checks against should be the start path.
     * But it will fall back on /.
     */
    private File getBackTop() {
        if (getArguments().containsKey(KEY_START_PATH)) {
            String keyStartPath = getArguments().getString(KEY_START_PATH);
            return getPath((keyStartPath != null ? keyStartPath : "/"));
        } else {
            return new File("/");
        }
    }

    /**
     * @return true if the current path is the startpath or /
     */
    public boolean isBackTop() {
        return 0 == compareFiles(mCurrentPath, getBackTop()) || 0 == compareFiles(mCurrentPath, new File("/"));
    }

    /**
     * Go up on level, same as pressing on "..".
     */
    public void goUp() {
        mCurrentPath = getParent(mCurrentPath);
        mCheckedItems.clear();
        mCheckedVisibleViewHolders.clear();
        refresh();
    }


}
