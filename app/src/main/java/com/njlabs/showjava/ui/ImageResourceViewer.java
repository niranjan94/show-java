package com.njlabs.showjava.ui;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.njlabs.showjava.R;
import com.njlabs.showjava.utils.TouchImageView;

import org.apache.commons.io.FilenameUtils;

public class ImageResourceViewer extends BaseActivity {

    private String sourceFilePath;
    private String sourceFilename;
    private boolean isBlack = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_image_resource_viewer);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        ActionBar actionBar = getSupportActionBar();

        Bundle extras = getIntent().getExtras();
        String packageID = "";
        if (extras != null) {
            sourceFilePath = extras.getString("file_path");
            sourceFilename = FilenameUtils.getName(sourceFilePath);
            packageID = extras.getString("package_id");
        }

        if (actionBar != null) {
            actionBar.setTitle(sourceFilename);
            String subtitle = FilenameUtils.getFullPath(sourceFilePath).replace(Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + packageID + "/", "");
            actionBar.setSubtitle(subtitle);
            if (sourceFilename.trim().equalsIgnoreCase("icon.png")) {
                actionBar.setSubtitle(packageID);
            }
        }

        TouchImageView touchImageView = (TouchImageView) findViewById(R.id.image_view);
        touchImageView.setImageDrawable(Drawable.createFromPath(sourceFilePath));

        touchImageView.setZoom(0.3f);
        touchImageView.setMinZoom(0.3f);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.invert_colors);
        item.setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invert_colors:
                if (isBlack) {
                    getWindow().getDecorView().setBackgroundColor(Color.WHITE);
                } else {
                    getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                }
                isBlack = !isBlack;
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
