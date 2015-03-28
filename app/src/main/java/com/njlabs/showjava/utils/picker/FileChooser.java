package com.njlabs.showjava.utils.picker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.njlabs.showjava.R;
import com.njlabs.showjava.ui.BaseActivity;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileChooser extends BaseActivity {
    private File currentDir;
    private FileArrayAdapter adapter;
    private FileFilter fileFilter;
    private File fileSelected;
    private ArrayList<String> extensions;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setupLayout(R.layout.activity_file_chooser, "Pick a file");

        if (extras != null) {
            if (extras.getStringArrayList("filterFileExtension") != null) {
                extensions = extras.getStringArrayList("filterFileExtension");
                fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return ((pathname.isDirectory()) || (pathname.getName().contains(".") && extensions.contains(pathname.getName().substring(pathname.getName().lastIndexOf(".")))));
                    }
                };
            }
        }
        listView = (ListView) findViewById(R.id.listView);
        currentDir = new File(Environment.getExternalStorageDirectory().getPath());
        fill(currentDir);
    }

    private void setupAdapter() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Option o = adapter.getItem(position);
                if (o.isFolder() || o.isParent()) {
                    currentDir = new File(o.getPath());
                    fill(currentDir);
                } else {
                    fileSelected = new File(o.getPath());
                    Intent intent = new Intent();
                    intent.putExtra("fileSelected", fileSelected.getAbsolutePath());
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((!currentDir.getName().equals("sdcard")) && (currentDir.getParentFile() != null)) {
                currentDir = currentDir.getParentFile();
                fill(currentDir);
            } else {
                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void fill(File f) {
        File[] dirs = null;
        if (fileFilter != null)
            dirs = f.listFiles(fileFilter);
        else
            dirs = f.listFiles();

        this.setTitle(getString(R.string.currentDir) + ": " + f.getName());
        List<Option> dir = new ArrayList<>();
        List<Option> fls = new ArrayList<>();
        try {
            for (File ff : dirs) {
                if (ff.isDirectory() && !ff.isHidden())
                    dir.add(new Option(ff.getName(), getString(R.string.folder), ff.getAbsolutePath(), true, false));
                else {
                    if (!ff.isHidden())
                        fls.add(new Option(ff.getName(), getString(R.string.fileSize) + ": "
                                + ff.length(), ff.getAbsolutePath(), false, false));
                }
            }
        } catch (Exception ignored) {

        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if (!f.getName().equalsIgnoreCase("sdcard")) {
            if (f.getParentFile() != null)
                dir.add(0, new Option("..", getString(R.string.parentDirectory), f.getParent(), false, true));
        }
        adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view,dir);
        listView.setAdapter(adapter);
        setupAdapter();
    }

}