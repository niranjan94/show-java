package com.njlabs.showjava.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.njlabs.showjava.ui.JavaExplorer;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Niranjan on 27-05-2015.
 */
@SuppressWarnings({"FieldCanBeLocal", "StringBufferReplaceableByString"})
public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

    private final Context myContext;
    private final String LINE_SEPARATOR = "\n";
    private final String sourceDir;
    private final String packageID;

    public ExceptionHandler(Context context, String sourceDir, String packageID) {
        this.myContext = context;
        this.sourceDir = sourceDir;
        this.packageID = packageID;
    }

    public void uncaughtException(Thread thread, Throwable exception) {

        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder errorReport = new StringBuilder();
        errorReport.append("************ CAUSE OF ERROR ************\n\n");
        errorReport.append(stackTrace.toString());

        errorReport.append("\n************ DEVICE INFORMATION ***********\n");
        errorReport.append("Brand: ");
        errorReport.append(Build.BRAND);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Device: ");
        errorReport.append(Build.DEVICE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Model: ");
        errorReport.append(Build.MODEL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Id: ");
        errorReport.append(Build.ID);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Product: ");
        errorReport.append(Build.PRODUCT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n************ FIRMWARE ************\n");
        errorReport.append("SDK: ");
        errorReport.append(Build.VERSION.SDK_INT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Release: ");
        errorReport.append(Build.VERSION.RELEASE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Incremental: ");
        errorReport.append(Build.VERSION.INCREMENTAL);
        errorReport.append(LINE_SEPARATOR);

        Log.e("com.njlabs.showjava", errorReport.toString());

        Toast.makeText(myContext, "There was an error decompiling this app. Showing incomplete source.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(myContext, JavaExplorer.class);
        intent.putExtra("java_source_dir", sourceDir);
        intent.putExtra("package_id", packageID);
        myContext.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

}
