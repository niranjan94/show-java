package com.njlabs.showjava.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.njlabs.showjava.R;
import com.njlabs.showjava.modals.Item;
import com.njlabs.showjava.utils.FileArrayAdapter;
import com.njlabs.showjava.utils.StringUtils;
import com.njlabs.showjava.utils.ZipUtils;
import com.njlabs.showjava.utils.logging.Ln;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaExplorer extends BaseActivity {

    private ListView lv;
    private String packageID;
    private ActionBar actionBar;
    private File currentDir;
    private FileArrayAdapter adapter;
    private String sourceDir;
    private ProgressDialog zipProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_app_listing);

        actionBar = getSupportActionBar();

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            sourceDir = extras.getString("java_source_dir");
            packageID = extras.getString("package_id");

            if (sourceDir != null) {
                lv = (ListView) findViewById(R.id.list);
                currentDir = new File(sourceDir);
                fill(currentDir);
            } else {
                finish();
            }
        }
    }

    private void fill(File f) {

        File[] dirs = f.listFiles();

        if (actionBar != null) {
            if (f.getName().equalsIgnoreCase("java_output")) {
                actionBar.setTitle("Viewing the source of " + packageID);
            } else {
                actionBar.setTitle(f.getName());
            }
        }

        List<Item> dir = new ArrayList<>();
        List<Item> fls = new ArrayList<>();
        try {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if (ff.isDirectory()) {
                    File[] fbuf = ff.listFiles();
                    int buf;
                    if (fbuf != null) {
                        buf = fbuf.length;
                    } else buf = 0;
                    String num_item = String.valueOf(buf);
                    if (buf == 0) num_item = num_item + " item";
                    else num_item = num_item + " items";
                    dir.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), R.drawable.viewer_folder));
                } else {
                    String extension = FilenameUtils.getExtension(ff.getName());
                    String fileSize = StringUtils.humanReadableByteCount(ff.length(), true);
                    if (extension.equalsIgnoreCase("java")) {
                        fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(), R.drawable.viewer_java));
                    } else if (extension.equalsIgnoreCase("xml")) {
                        fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(), R.drawable.viewer_xml));
                    } else if (extension.equalsIgnoreCase("txt")) {
                        fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(),R.drawable.viewer_summary));
                    } else if (extension.equalsIgnoreCase("png") | extension.equalsIgnoreCase("jpg")) {
                        fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(), R.drawable.viewer_image));
                    }
                }
            }
        } catch (Exception e) {
            Ln.d(e);
        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);

        if (!f.equals(new File(sourceDir)))
            dir.add(0, new Item("..", "Parent Directory", "", f.getParent(), R.drawable.directory_up));

        adapter = new FileArrayAdapter(JavaExplorer.this, R.layout.java_explorer_list_item, dir);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item o = adapter.getItem(position);
                if (o.getImage() == R.drawable.viewer_folder || o.getImage() == R.drawable.directory_up) {
                    currentDir = new File(o.getPath());
                    fill(currentDir);
                } else {
                    onFileClick(o);
                }
            }
        });
    }

    private void onFileClick(Item o) {
        if (FilenameUtils.getExtension(o.getPath()).equals("png") || FilenameUtils.getExtension(o.getPath()).equals("jpg")) {
            Intent i = new Intent(getApplicationContext(), ImageResourceViewer.class);
            i.putExtra("file_path", o.getPath());
            i.putExtra("package_id", packageID);
            startActivity(i);
        } else {
            Intent i = new Intent(getApplicationContext(), SourceViewer.class);
            i.putExtra("file_path", o.getPath());
            i.putExtra("package_id", packageID);
            startActivity(i);
        }
    }

    private void showProgressDialog() {
        if (zipProgressDialog == null) {
            zipProgressDialog = new ProgressDialog(this);
            zipProgressDialog.setIndeterminate(false);
            zipProgressDialog.setCancelable(false);
            zipProgressDialog.setInverseBackgroundForced(false);
            zipProgressDialog.setCanceledOnTouchOutside(false);
            zipProgressDialog.setMessage("Loading installed applications...");
        }
        zipProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (zipProgressDialog != null && zipProgressDialog.isShowing()) {
            zipProgressDialog.dismiss();
        }
    }

    private class SourceArchiver extends AsyncTask<String, String, File> {

        @Override
        protected File doInBackground(String... params) {
            publishProgress("Compressing source files ...");
            return ZipUtils.zipDir(new File(sourceDir), packageID);
        }

        @Override
        protected void onPostExecute(File zipFilePath) {
            dismissProgressDialog();
            shareSourceZip(zipFilePath);
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onProgressUpdate(String... text) {
            zipProgressDialog.setMessage(text[0]);
        }
    }
    @Override
    public void onBackPressed() {
        if (!currentDir.equals(new File(sourceDir))) {
            currentDir = new File(currentDir.getParent());
            fill(currentDir);
        } else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
            finish();
        }
    }

    private void shareSourceZip(File zipFile){
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile));
        shareIntent.setType("application/zip");
        startActivity(Intent.createChooser(shareIntent, "Send source via"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.explorer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_delete:
                new AlertDialog.Builder(this)
                        .setMessage("Are you sure want to delete ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteSource();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();

                return true;

            case R.id.action_share:
                SourceArchiver sourceArchiver = new SourceArchiver();
                sourceArchiver.execute();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteSource() {
        try {
            final File sourceDir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + packageID);
            if (sourceDir.exists()) {
                FileUtils.deleteDirectory(sourceDir);
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
        Toast.makeText(baseContext, "The source code has been deleted from sdcard", Toast.LENGTH_SHORT).show();
        finish();
    }

}