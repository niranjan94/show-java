package com.njlabs.showjava.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.common.html.HtmlEscapers;
import com.njlabs.showjava.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SourceViewer extends BaseActivity {

	String sourceFilePath;
	String sourceFilename;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupLayout(R.layout.activity_source_viewer);
		getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        ActionBar actionBar = getSupportActionBar();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
            sourceFilePath = extras.getString("file_path");
            sourceFilename = extras.getString("file_name");
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

        if(actionBar!=null) {
            actionBar.setTitle(sourceFilename);
            actionBar.setSubtitle(sourceFilePath.replace(Environment.getExternalStorageDirectory()+"/ShowJava/sources/",""));
        }

    	FileInputStream fs;
    	int ch;
    	StringBuilder str = new StringBuilder();
    	String sourceCodeText = "";
    	try
    	{
    		fs = new FileInputStream(new File(sourceFilePath, sourceFilename));
    		while ((ch = fs.read()) != -1) {
				str.append((char) ch);
			}
			sourceCodeText = str.toString();
			fs.close();
		} catch (IOException ignored) {

    	}

        sourceCodeText = HtmlEscapers.htmlEscaper().escape(sourceCodeText);

        WebView webView = (WebView) findViewById(R.id.source_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
            	ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
            	progress.setVisibility(View.GONE);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                InputStream stream = inputStreamForAndroidResource(url);
                if (stream != null) {
                    return new WebResourceResponse("text/javascript", "utf-8", stream);
                }
                return super.shouldInterceptRequest(view, url);
            }

            private InputStream inputStreamForAndroidResource(String url) {
                final String ANDROID_ASSET = "file:///android_asset/";

                if (url.contains(ANDROID_ASSET)) {
                    url = url.replaceFirst(ANDROID_ASSET, "");
                    try {
                        AssetManager assets = getAssets();
                        Uri uri = Uri.parse(url);
                        return assets.open(uri.getPath(), AssetManager.ACCESS_STREAMING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        });

        webView.loadDataWithBaseURL("file:///android_asset/","<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><script src=\"run_prettify.js?skin=sons-of-obsidian\"></script></head><body bgcolor=\"#000000\"><pre class=\"prettyprint linenums\">"+sourceCodeText+"</pre></body></html>", "text/html", "UTF-8",null);	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
