package com.njlabs.showjava;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.DateFormat;

import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class JavaExplorer extends ListActivity {

    private File currentDir;
    private FileArrayAdapter adapter;
    
    String PackageID;
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        String JavSourceDir=null;
        if (extras != null) {
            JavSourceDir = extras.getString("java_source_dir");
            PackageID = extras.getString("package_id");
        }
        getActionBar().setIcon(R.drawable.ic_action_bar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        currentDir = new File(JavSourceDir);
        fill(currentDir);
    }
    private void fill(File f)
    {
    	File[]dirs = f.listFiles();
    	if(f.getName().equalsIgnoreCase("java_output"))
    	{
    		this.setTitle("Viewing the source of "+PackageID);
    	}
    	else
    	{
    		this.setTitle(f.getName());
    	}    	
    	List<Item>dir = new ArrayList<Item>();
    	List<Item>fls = new ArrayList<Item>();
    	try
    	{
    		for(File ff: dirs)
    		{
    			Date lastModDate = new Date(ff.lastModified());
    			DateFormat formater = DateFormat.getDateTimeInstance();
    			String date_modify = formater.format(lastModDate);
    			if(ff.isDirectory()){
    				File[] fbuf = ff.listFiles();
    				int buf = 0;
    				if(fbuf != null){
    					buf = fbuf.length;
    				}
    				else buf = 0;
    				String num_item = String.valueOf(buf);
    				if(buf == 0) num_item = num_item + " item";
    				else num_item = num_item + " items";
    				dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),"directory_icon"));
    			}
    			else
    			{
    				fls.add(new Item(ff.getName(),ff.length() + " Byte", date_modify, ff.getAbsolutePath(),"file_icon"));
    			}
    		}
    	}
    	catch(Exception e)
    	{   
                             
    	}
    	Collections.sort(dir);
    	Collections.sort(fls);
    	dir.addAll(fls);
    	if(!f.getName().equalsIgnoreCase("java_output"))
    		dir.add(0,new Item("..","Parent Directory","",f.getParent(),"directory_up"));
    	adapter = new FileArrayAdapter(JavaExplorer.this,R.layout.java_explorer_list_item,dir);
    	this.setListAdapter(adapter);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);
    	Item o = adapter.getItem(position);
    	if(o.getImage().equalsIgnoreCase("directory_icon")||o.getImage().equalsIgnoreCase("directory_up")){
    		currentDir = new File(o.getPath());
    		fill(currentDir);
    	}
    	else
    	{
    		onFileClick(o);
    	}
    }
    private void onFileClick(Item o)
    {
        Intent i = new Intent(getApplicationContext(), SourceViewer.class);
		i.putExtra("file_path",currentDir.toString());
		i.putExtra("file_name",o.getName());
		startActivity(i);
    }
    @Override
    public void onBackPressed()
    {
    	if(!currentDir.getName().equalsIgnoreCase("java_output"))
    	{
    		currentDir = new File(currentDir.getParent());
    		fill(currentDir);
    	}
    	else
    	{
    		Intent returnIntent = new Intent();
    		setResult(RESULT_CANCELED, returnIntent);        
    		finish();
    	}
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	if(!currentDir.getName().equalsIgnoreCase("java_output"))
            	{
            		currentDir = new File(currentDir.getParent());
            		fill(currentDir);
            	}
            	else
            	{
            		Intent returnIntent = new Intent();
            		setResult(RESULT_CANCELED, returnIntent);        
            		finish();
            	}
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}