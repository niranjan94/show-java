package com.njlabs.showjava.ui;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.google.common.html.HtmlEscapers;
import com.njlabs.showjava.R;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SourceViewer extends BaseActivity {

    private String sourceFilePath;
    private String sourceFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_source_viewer);
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
            if (sourceFilename.trim().equalsIgnoreCase("AndroidManifest.xml")) {
                actionBar.setSubtitle(packageID);
            }
        }

        FileInputStream fs;
        int ch;
        StringBuilder str = new StringBuilder();
        String sourceCodeText = "";
        try {
            fs = new FileInputStream(new File(sourceFilePath));
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

        webView.loadDataWithBaseURL("file:///android_asset/", "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><script src=\"run_prettify.js?skin=sons-of-obsidian\"></script></head><body bgcolor=\"#000000\"><pre class=\"prettyprint linenums\">" + sourceCodeText + "</pre></body></html>", "text/html", "UTF-8", null);
    }

}