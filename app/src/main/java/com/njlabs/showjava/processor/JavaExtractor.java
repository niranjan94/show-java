package com.njlabs.showjava.processor;

import android.os.Environment;
import android.os.Handler;

import com.njlabs.showjava.utils.ExceptionHandler;

import org.benf.cfr.reader.Main;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.io.File;

/**
 * Created by Niranjan on 29-05-2015.
 */
public class JavaExtractor extends ProcessServiceHelper {

    public JavaExtractor(ProcessService processService, Handler UIHandler, String packageDir, String packageID, ExceptionHandler exceptionHandler) {
        this.processService = processService;
        this.UIHandler = UIHandler;
        this.packageDir = packageDir;
        this.packageID = packageID;
        this.exceptionHandler = exceptionHandler;
    }

    public void extract(){

        broadcastStatus("jar2java");
        File JarInput;
        JarInput = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+packageID+"/"+packageID+".jar");
        final File JavaOutputDir = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+packageID+"/java_output");

        if (!JavaOutputDir.isDirectory())
        {
            JavaOutputDir.mkdirs();
        }

        processService.JavaOutputDir = JavaOutputDir.toString();
        String[] args = {JarInput.toString(), "--outputdir", JavaOutputDir.toString()};
        GetOptParser getOptParser = new GetOptParser();

        Options options = null;
        try
        {
            options = (Options) getOptParser.parse(args, OptionsImpl.getFactory());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        final DCCommonState dcCommonState = new DCCommonState(options);
        final String path = options != null ? options.getFileName() : null;

        ThreadGroup group = new ThreadGroup("Jar 2 Java Group");
        Thread javaExtractionThread = new Thread(group, new Runnable(){
            @Override
            public void run(){
                try
                {
                    Main.doJar(dcCommonState, path);

                }
                catch(Exception | StackOverflowError e)
                {
                    processService.publishProgress("start_activity_with_error");
                }
                processService.publishProgress("start_activity");
            }
        },"Jar to Java Thread", 20971520);
        javaExtractionThread.setPriority(Thread.MAX_PRIORITY);
        javaExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
        javaExtractionThread.start();
    }
}
