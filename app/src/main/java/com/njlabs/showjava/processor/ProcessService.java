package com.njlabs.showjava.processor;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.njlabs.showjava.Constants;
import com.njlabs.showjava.R;
import com.njlabs.showjava.ui.AppProcessActivity;
import com.njlabs.showjava.utils.ExceptionHandler;
import com.njlabs.showjava.utils.Notify;
import com.njlabs.showjava.utils.logging.Ln;

import net.dongliu.apk.parser.ApkParser;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
public class ProcessService extends IntentService {

    private String PackageId;
    private String PackageDir;
    private String PackageName;

    public String JavaOutputDir;

    private String filePath;
    private Handler UIHandler;

    private Notify processNotify;

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

        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(getApplicationContext(), AppProcessActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity( this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Decompiling")
                .setContentText("Processing the apk")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(resultPendingIntent);

        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(Constants.PROCESS_NOTIFICATION_ID, mBuilder.build());

        processNotify = new Notify(mNotifyManager,mBuilder,Constants.PROCESS_NOTIFICATION_ID);
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

            ApkParser apkParser = null;
            try {
                apkParser = new ApkParser(new File(filePath));
                if(PackageName == null){
                    PackageName = apkParser.getApkMeta().getLabel();
                }
                if(PackageId == null){
                    PackageId = apkParser.getApkMeta().getPackageName();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            String JavaOutputDir = Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+PackageId+"/java_output";

            ExceptionHandler exceptionHandler = new ExceptionHandler(getApplicationContext(),JavaOutputDir,PackageId);
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
            Processor.extract(this, UIHandler, PackageDir, PackageId, exceptionHandler);
        }

    }

    public void publishProgress(String progressText){
        switch (progressText) {
            case "start_activity": {
                broadcastStatusWithPackageInfo(progressText,JavaOutputDir + "/",PackageId);
                break;
            }
            case "start_activity_with_error": {
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

    public void broadcastStatus(String status){
        sendNotification(status,"");
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION).putExtra(Constants.PROCESS_STATUS_KEY, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    public void broadcastStatus(String statusKey, String statusData){
        sendNotification(statusKey,statusData);
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                                    .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                                    .putExtra(Constants.PROCESS_STATUS_MESSAGE, statusData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    public void broadcastStatusWithPackageInfo(String statusKey, String dir, String packId){
        sendNotification(statusKey,"");
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                .putExtra(Constants.PROCESS_DIR, dir)
                .putExtra(Constants.PROCESS_PACKAGE_ID, packId);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void sendNotification(String statusKey, String statusData){
        switch(statusKey){
            case "optimise_dex_start":
                processNotify.updateTitleText("Optimising dex file", "");
                break;
            case "optimising":
                processNotify.updateTitleText("Optimising dex file", statusData);
                break;
            case "optimise_dex_finish":
                processNotify.updateTitleText("Finishing optimisation", "");
                break;
            case "merging_classes":
                processNotify.updateTitleText("Merging classes", "");
                break;

            case "start_activity":
                processNotify.cancel();
                break;

            case "start_activity_with_error":
                processNotify.cancel();
                break;

            case "exit_process_on_error":
                processNotify.cancel();
                break;
            case "finaldex":
                processNotify.updateTitleText("Finishing optimisation", "");
                break;
            case "dex2jar":
                processNotify.updateTitleText("Decompiling dex to jar", "");
                break;
            case "jar2java":
                processNotify.updateTitleText("Decompiling to java", "");
                break;
            case "exit":
                processNotify.cancel();
                break;

            default:
                processNotify.updateText(statusData);
        }
    }
}
