package com.njlabs.showjava.processor;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;
import com.njlabs.showjava.Constants;
import com.njlabs.showjava.modals.DecompileHistoryItem;
import com.njlabs.showjava.utils.DatabaseHandler;
import com.njlabs.showjava.utils.logging.Ln;

import org.benf.cfr.reader.Main;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
public class ProcessService extends IntentService {

    private String PackageId;
    private String PackageDir;
    private String PackageName;

    private DatabaseHandler db;
    private Boolean isJar = false;

    private String JavaOutputDir;

    private String filePath;
    private Handler UIHandler;

    public ProcessService() {
        super("ProcessService");
    }

    private class ToastRunnable implements Runnable {
        String mText;
        public ToastRunnable(String text) {
            mText = text;
        }
        @Override
        public void run(){
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }
    }

    public void onCreate() {
        super.onCreate();
        Log.d("Server", ">>>onCreate()");
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Ln.d("onStartCommand ProcessService");
        UIHandler = new Handler();
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Ln.d("onHandleIntent ProcessService");
        Bundle extras = workIntent.getExtras();
        if (extras != null) {
            PackageName = extras.getString("package_name");
            PackageId = extras.getString("package_id");
            PackageDir = extras.getString("package_dir");
        }
        db = new DatabaseHandler(this);
        if(!isJar)
        {
            ThreadGroup group = new ThreadGroup("Optimise Dex Group");
            broadcastStatus("optimise_dex_start");
            Runnable runProcess=new Runnable(){
                @Override
                public void run(){

                    DexFile dexFile = null;
                    try
                    {
                        dexFile = DexFileFactory.loadDexFile(PackageDir, 19);
                    } catch (Exception e){
                        broadcastStatus("exit");
                        UIHandler.post(new ToastRunnable("The app you selected cannot be decompiled. Please select another app."));
                    }
                    List<ClassDef> classes = new ArrayList<>();
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
                            broadcastStatus("optimising", CurrentClass.replaceAll("Processing ", ""));
                            classes.add(classDef);
                        }
                    }

                    broadcastStatus("optimise_dex_finish");

                    File WorkingDirectory=new File(Environment.getExternalStorageDirectory() + "/ShowJava");
                    File PerAppWorkingDirectory=new File(WorkingDirectory + "/" + PackageId);
                    PerAppWorkingDirectory.mkdirs();
                    Log.d("DEBUGGER", "Prepare Writing");

                    broadcastStatus("merging_classes");

                    dexFile = new ImmutableDexFile(classes);
                    try
                    {
                        Log.d("DEBUGGER","Start Writing");
                        DexFileFactory.writeDexFile(PerAppWorkingDirectory+"/optimised_classes.dex", dexFile);
                        Log.d("DEBUGGER","Writing done!");
                    }
                    catch (IOException e)
                    {
                        broadcastStatus("exit");
                        UIHandler.post(new ToastRunnable("The app you selected cannot be decompiled. Please select another app."));
                    }
                    executeProcess();
                }
            };
            new Thread(group, runProcess, "Optimise Dex Thread", 10485760).start();
        }
        else
        {
            executeProcess();
        }
    }

    private void executeProcess(){
        ExtractJar();
        DecompileJar();
    }

    private void publishProgress(String progressText){
        switch (progressText) {
            case "start_activity": {
                if (!db.packageExistsInHistory(PackageId))
                    db.addHistoryItem(new DecompileHistoryItem(PackageId, PackageName, DateFormat.getDateInstance().format(new Date())));
                broadcastStatusWithPackageInfo(progressText,JavaOutputDir + "/",PackageId);
                break;
            }
            case "start_activity_with_error": {
                if (!db.packageExistsInHistory(PackageId))
                    db.addHistoryItem(new DecompileHistoryItem(PackageId, PackageName, DateFormat.getDateInstance().format(new Date())));
                broadcastStatusWithPackageInfo(progressText, JavaOutputDir + "/", PackageId);
                UIHandler.post(new ToastRunnable("Decompilation completed with errors. This incident has been reported to the developer."));
                break;
            }
            case "exit_process_on_error":
                broadcastStatus(progressText);
                UIHandler.post(new ToastRunnable("The app you selected cannot be decompiled. Please select another app."));
                break;
            default:
                break;
        }
    }

    private void ExtractJar()
    {
        Log.i("STATUS", "Jar Extraction Started");

        publishProgress("dex2jar");

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

        File WorkingDirectory=new File(Environment.getExternalStorageDirectory() + "/ShowJava");
        File PerAppWorkingDirectory=new File(WorkingDirectory + "/" + PackageId);
        File file = new File(PerAppWorkingDirectory+"/"+PackageId+ ".jar");

        try
        {
            DexFileReader reader = new DexFileReader(new File(PerAppWorkingDirectory+"/optimised_classes.dex"));
            Dex2jar.from(reader).reUseReg(reuseReg).topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
                    .optimizeSynchronized(optimizeSynchronized).printIR(printIR).verbose(verbose).to(file);
        } catch (Exception e) {
            publishProgress("exit_process_on_error");
        }
        Log.i("STATUS","Clearing cache");
        File ClassDex=new File(PerAppWorkingDirectory+"/optimised_classes.dex");
        ClassDex.delete();
    }
    private void DecompileJar()
    {
        broadcastStatus("jar2java");
        File JarInput;
        if(!isJar)
        {
            JarInput = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+PackageId+"/"+PackageId+".jar");
        }
        else
        {
            JarInput = new File(PackageDir);
        }

        final File JavaOutputDir = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+PackageId+"/java_output");

        if (!JavaOutputDir.isDirectory())
        {
            JavaOutputDir.mkdirs();
        }

        this.JavaOutputDir=JavaOutputDir.toString();
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
        new Thread(group, new Runnable(){
            @Override
            public void run(){
                try
                {
                    Main.doJar(dcCommonState, path);

                }
                catch(Exception | StackOverflowError e)
                {
                    publishProgress("start_activity_with_error");
                }
                publishProgress("start_activity");
            }
        }, "Jar to Java Thread", 20971520).start();
    }
    private void broadcastStatus(String status){
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION).putExtra(Constants.PROCESS_STATUS_KEY, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    private void broadcastStatus(String statusKey, String statusData){
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                                    .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                                    .putExtra(Constants.PROCESS_STATUS_MESSAGE, statusData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    private void broadcastStatusWithPackageInfo(String statusKey, String dir, String packId){
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                .putExtra(Constants.PROCESS_DIR, dir)
                .putExtra(Constants.PROCESS_PACKAGE_ID, packId);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    public class ProgressStream extends OutputStream {
        public ProgressStream() {

        }
        public void write(@NonNull byte[] data,int i1,int i2)
        {
            String str = new String(data);
            str = str.replace("\n", "").replace("\r", "");
            if(!str.equals("")&&str!=null&&!str.equals("")) {
                broadcastStatus("progress_stream",str);
            }
        }
        @Override
        public void write(int arg0) throws IOException {
            // TODO Auto-generated method stub

        }
    }
}
