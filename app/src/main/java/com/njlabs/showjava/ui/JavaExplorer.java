package com.njlabs.showjava.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
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

    private File currentDir;
    private FileArrayAdapter adapter;

    private String rootDir;

    ListView lv;
    String PackageID;
    ActionBar actionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setupLayout(R.layout.activity_app_listing);

        actionBar = getSupportActionBar();

        Bundle extras = getIntent().getExtras();
        String JavSourceDir;

        if (extras != null) {
            JavSourceDir = extras.getString("java_source_dir");
            PackageID = extras.getString("package_id");

            if(JavSourceDir != null){
                lv = (ListView) findViewById(R.id.list);
                currentDir = new File(JavSourceDir);
                rootDir = currentDir.toString();
                fill(currentDir);
            } else {
                finish();
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        }


    }
    private void fill(File f) {

    	File[]dirs = f.listFiles();

        if(actionBar!=null){
            if(f.getName().equalsIgnoreCase("java_output")) {
                actionBar.setTitle("Viewing the source of " + PackageID);
            }
            else {
                actionBar.setTitle(f.getName());
            }
        }

    	List<Item>dir = new ArrayList<>();
    	List<Item>fls = new ArrayList<>();
    	try
    	{
    		for(File ff: dirs)
    		{
    			Date lastModDate = new Date(ff.lastModified());
    			DateFormat formater = DateFormat.getDateTimeInstance();
    			String date_modify = formater.format(lastModDate);
    			if(ff.isDirectory()){
    				File[] fbuf = ff.listFiles();
    				int buf;
    				if(fbuf != null){
    					buf = fbuf.length;
    				}
    				else buf = 0;
    				String num_item = String.valueOf(buf);
    				if(buf == 0) num_item = num_item + " item";
    				else num_item = num_item + " items";
    				dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(), "viewer_folder"));
    			}
    			else {
                    String extension = FilenameUtils.getExtension(ff.getName());
					String fileSize = StringUtils.humanReadableByteCount(ff.length(), true);
                    if(extension.equalsIgnoreCase("java")){
                        fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(),"viewer_java"));
                    } else if(extension.equalsIgnoreCase("xml")){
                        fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(),"viewer_xml"));
                    } else if(extension.equalsIgnoreCase("txt")){
						fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(),"viewer_summary"));
                    } else if(extension.equalsIgnoreCase("png")|extension.equalsIgnoreCase("jpg")){
                        fls.add(new Item(ff.getName(), fileSize, date_modify, ff.getAbsolutePath(),"viewer_image"));
                    }
    			}
    		}
    	} catch(Exception e) {
            Crashlytics.logException(e);
    	}
    	Collections.sort(dir);
    	Collections.sort(fls);
    	dir.addAll(fls);

    	if(!f.toString().equalsIgnoreCase(rootDir))
    		dir.add(0,new Item("..","Parent Directory","",f.getParent(),"directory_up"));

    	adapter = new FileArrayAdapter(JavaExplorer.this, R.layout.java_explorer_list_item,dir);
    	lv.setAdapter(adapter);
    	lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Item o = adapter.getItem(position);
		    	if(o.getImage().equalsIgnoreCase("viewer_folder")||o.getImage().equalsIgnoreCase("directory_up")){
		    		currentDir = new File(o.getPath());
		    		fill(currentDir);
		    	}
		    	else {
		    		onFileClick(o);
		    	}
				
			}
		});
    }

    private void onFileClick(Item o) {
        if (FilenameUtils.getExtension(o.getPath()).equals("png")||FilenameUtils.getExtension(o.getPath()).equals("jpg")){
            Intent i = new Intent(getApplicationContext(), ImageResourceViewer.class);
            i.putExtra("file_path", o.getPath());
            i.putExtra("package_id",PackageID);
            startActivity(i);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        } else {
            Intent i = new Intent(getApplicationContext(), SourceViewer.class);
            i.putExtra("file_path", o.getPath());
            i.putExtra("package_id",PackageID);
            startActivity(i);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        }
    }

    @Override
    public void onBackPressed() {
    	if(!currentDir.toString().equalsIgnoreCase(rootDir)) {
    		currentDir = new File(currentDir.getParent());
    		fill(currentDir);
    	} else {
    		Intent returnIntent = new Intent();
    		setResult(RESULT_CANCELED, returnIntent);        
    		finish();
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    	}
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
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                return true;

            case R.id.action_delete:
                try {
                    final File sourceDir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + PackageID);
                    if(sourceDir.exists()){
                        FileUtils.deleteDirectory(sourceDir);
                    }
                } catch (IOException e) {
                    Crashlytics.logException(e);
                }
                Toast.makeText(baseContext, "The source code has been deleted from sdcard", Toast.LENGTH_SHORT).show();
                finish();
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}