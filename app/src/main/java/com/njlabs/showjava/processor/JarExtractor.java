package com.njlabs.showjava.processor;

import android.util.Log;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Niranjan on 29-05-2015.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
public class JarExtractor extends ProcessServiceHelper {

    public JarExtractor(ProcessService processService) {
        this.processService = processService;
        this.UIHandler = processService.UIHandler;
        this.packageFilePath = processService.packageFilePath;
        this.packageName = processService.packageName;
        this.exceptionHandler = processService.exceptionHandler;
        this.sourceOutputDir = processService.sourceOutputDir;
        this.javaSourceOutputDir = processService.javaSourceOutputDir;
    }

    public void extract(){
        ThreadGroup group = new ThreadGroup("DEX TO JAR EXTRACTION");
        broadcastStatus("optimise_dex_start");
        Runnable runProcess = new Runnable(){
            @Override
            public void run(){
                apkToDex();
                dexToJar();
                startJavaExtractor();
            }
        };
        Thread extractionThread = new Thread (group, runProcess, "DEX TO JAR EXTRACTION", 10485760);
        extractionThread.setPriority(Thread.MAX_PRIORITY);
        extractionThread.setUncaughtExceptionHandler(exceptionHandler);
        extractionThread.start();
    }

    public void apkToDex(){
        DexFile dexFile = null;
        try {
            dexFile = DexFileFactory.loadDexFile(packageFilePath, 19);
        } catch (Exception e){
            broadcastStatus("exit");
            UIHandler.post(new ToastRunnable("The app you selected cannot be decompiled. Please select another app."));
        }
        List<ClassDef> classes = new ArrayList<>();
        broadcastStatus("optimising", "");
        for (ClassDef classDef : dexFile.getClasses()) {
            if (
                    classDef.getType().startsWith("Lcom/google/apps/")
                            ||!classDef.getType().startsWith("Landroid/support/")
                            &&!classDef.getType().startsWith("Lcom/onemarker/")
                            &&!classDef.getType().startsWith("Lcom/androidquery/")
                            &&!classDef.getType().startsWith("Lcom/parse/")
                            &&!classDef.getType().startsWith("Lcom/android/")
                            &&!classDef.getType().startsWith("Lcom/actionbarsherlock/")
                            &&!classDef.getType().startsWith("Lorg/apache/")
                            &&!classDef.getType().startsWith("Lorg/acra/")
                            &&!classDef.getType().startsWith("Ljavax/")
                            &&!classDef.getType().startsWith("Lorg/joda/")
                            &&!classDef.getType().startsWith("Lorg/antlr/")
                            &&!classDef.getType().startsWith("Ljunit/")
                            &&!classDef.getType().startsWith("Lorg/codehaus/jackson/")
                            &&!classDef.getType().startsWith("Lcom/fasterxml/")
                            &&!classDef.getType().startsWith("Lcom/google/")
                            &&!classDef.getType().startsWith("Lnet/sourceforge/")
                            &&!classDef.getType().startsWith("Lorg/achartengine/")
                            &&!classDef.getType().startsWith("Lcom/bugsense/")
                            &&!classDef.getType().startsWith("Lorg/andengine/")
                            &&!classDef.getType().startsWith("Lcom/inmobi/")
                            &&!classDef.getType().startsWith("Landroid/")
                            &&!classDef.getType().startsWith("Lcom/google/android/gms/")
                            &&!classDef.getType().startsWith("Lcom/google/api/")) {
                final String CurrentClass=classDef.getType();
                broadcastStatus("optimising_class", CurrentClass.replaceAll("Processing ", ""));
                classes.add(classDef);
            }
        }
        broadcastStatus("optimise_dex_finish");

        File PerAppWorkingDirectory = new File(processService.sourceOutputDir);
        PerAppWorkingDirectory.mkdirs();

        Log.d("DEBUGGER", "Prepare Writing");

        broadcastStatus("merging_classes");

        dexFile = new ImmutableDexFile(classes);
        try {
            Log.d("DEBUGGER","Start Writing");
            DexFileFactory.writeDexFile(PerAppWorkingDirectory+"/optimised_classes.dex", dexFile);
            Log.d("DEBUGGER","Writing done!");
        }
        catch (IOException e) {
            broadcastStatus("exit");
            UIHandler.post(new ToastRunnable("The app you selected cannot be decompiled. Please select another app."));
        }
    }
    public void dexToJar(){
        Log.i("STATUS", "Jar Extraction Started");

        broadcastStatus("dex2jar");

        // DEX 2 JAR CONFIGS
        boolean reuseReg = false; // reuse register while generate java .class file
        boolean topologicalSort1 = false; // same with --topological-sort/-ts
        boolean topologicalSort = false; // sort block by topological, that will generate more readable code
        boolean verbose = true; // show progress
        boolean debugInfo = false; // translate debug info
        boolean printIR = false; // print ir to System.out
        boolean optimizeSynchronized = true; // Optimise-synchronised

        //////
        PrintStream printStream = new PrintStream(new ProgressStream());
        System.setErr(printStream);
        System.setOut(printStream);
        //////

        File PerAppWorkingDirectory = new File(sourceOutputDir);
        File file = new File(PerAppWorkingDirectory+"/"+ packageName + ".jar");

        try {
            DexFileReader reader = new DexFileReader(new File(PerAppWorkingDirectory+"/optimised_classes.dex"));
            Dex2jar.from(reader).reUseReg(reuseReg).topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
                    .optimizeSynchronized(optimizeSynchronized).printIR(printIR).verbose(verbose).to(file);
        } catch (Exception e) {
            broadcastStatus("exit_process_on_error");
        }

        Log.i("STATUS","Clearing cache");
        File ClassDex = new File(PerAppWorkingDirectory+"/optimised_classes.dex");
        ClassDex.delete();
    }

    private void startJavaExtractor(){
        JavaExtractor javaExtractor = new JavaExtractor(processService);
        javaExtractor.extract();
    }
}
