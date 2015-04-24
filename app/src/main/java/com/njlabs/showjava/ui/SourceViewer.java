package com.njlabs.showjava.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.njlabs.showjava.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SourceViewer extends BaseActivity {

	String FilePath;
	String FileName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupLayout(R.layout.activity_source_viewer);
		getWindow().getDecorView().setBackgroundColor(Color.BLACK);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
            FilePath = extras.getString("file_path");
            FileName = extras.getString("file_name");
        }
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH || Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 ){
            new AlertDialog.Builder(this)
                    .setMessage("Source code cannot be displayed properly on devices running Android 4.0.x (Icecream Sandwich) due to a bug present in the operating system. But you can directly view the source code from the 'ShowJava' folder in your sdcard. Inconvenience is regretted.")
                    .setPositiveButton("Oh ! That Sucks !", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "yea ! I know :) Please update your phone.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
	    getSupportActionBar().setTitle(FileName);

		WebView webView = (WebView) findViewById(R.id.source_view);
        
    	FileInputStream fs;
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
		} catch (IOException ignored) {

    	}
    	
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
            	ProgressBar progress=(ProgressBar) findViewById(R.id.progress);
            	progress.setVisibility(View.GONE);
            }
        });
        webView.loadDataWithBaseURL("file:///android_asset/","<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/prettify.css\"><script src=\"file:///android_asset/run_prettify.js?skin=sons-of-obsidian\"></script></head><body bgcolor=\"#000000\"><pre class=\"prettyprint linenums\">"+ReadText+"</pre></body></html>", "text/html", "UTF-8",null);
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            finish();
	            return true;
	            
	        case R.id.about_option:
	        	Intent i=new Intent(getBaseContext(),About.class);
	        	startActivity(i);
	        	return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

}
