package com.njlabs.showjava.ui;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.njlabs.showjava.R;
import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;

import java.io.File;

public class FilePicker extends AbstractFilePickerActivity<File> {

    FilePickerFragment currentFragment;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }


    protected AbstractFilePickerFragment<File> getFragment(String startPath, int mode, boolean allowMultiple, boolean allowCreateDir) {
        currentFragment = new FilePickerFragment();
        currentFragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(), mode, allowMultiple, allowCreateDir);
        return currentFragment;
    }

    /**
     * Override the back-button.
     */
    @Override
    public void onBackPressed() {
        // If at top most level, normal behaviour
        if (currentFragment == null || currentFragment.isBackTop()) {
            finish();
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        } else {
            // Else go up
            currentFragment.goUp();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
