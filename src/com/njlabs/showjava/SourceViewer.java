package com.njlabs.showjava;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

public class SourceViewer extends Activity {

	String FilePath;
	String FileName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_source_viewer);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
            FilePath = extras.getString("file_path");
            FileName = extras.getString("file_name");
        }
		getActionBar().setTitle(FileName);
		getActionBar().setIcon(R.drawable.ic_action_bar);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		WebView webView = (WebView) findViewById(R.id.source_view);
        
    	FileInputStream fs = null;
    	///
    	/// READ FROM EXTRENAL MEMORY
    	///
    	int ch;
    	StringBuilder str = new StringBuilder();
    	String ReadText=null;
    	// GET EXTERNAL STORAGE DIRECTORY FILE PATH
    	try 
    	{
    		fs = new FileInputStream(new File(FilePath,FileName));
    		while ((ch = fs.read()) != -1)
			{
				str.append((char) ch);
			}
			ReadText=str.toString();
			fs.close();
		}
    	catch (FileNotFoundException e) 
    	{
			
		}
    	catch (IOException e) 
    	{

    	}
    	
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.loadDataWithBaseURL("file:///android_asset/","<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/prettify.css\"><script src=\"file:///android_asset/run_prettify.js?skin=sons-of-obsidian\"></script></head><body bgcolor=\"#000000\"><pre class=\"prettyprint linenums\">"+ReadText+"</pre></body></html>", "text/html", "UTF-8",null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            finish();
	            return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

}
