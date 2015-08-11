package com.njlabs.showjava.processor;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.njlabs.showjava.Constants;
import com.njlabs.showjava.R;
import com.njlabs.showjava.ui.AppProcessActivity;
import com.njlabs.showjava.utils.ExceptionHandler;
import com.njlabs.showjava.utils.Notify;
import com.njlabs.showjava.utils.SourceInfo;
import com.njlabs.showjava.utils.logging.Ln;

import net.dongliu.apk.parser.ApkParser;

import java.io.File;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
public class ProcessService extends IntentService {

    public String packageName;
    public String packageFilePath;
    public String packageLabel;

    public String javaSourceOutputDir;
    public String sourceOutputDir;
    public ExceptionHandler exceptionHandler;

    public Handler UIHandler;

    public Notify processNotify;
    public ApkParser apkParser;

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

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Decompiling")
                .setContentText("Processing the apk")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setAutoCancel(false);

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

            packageFilePath = extras.getString("package_file_path");
            Ln.d("package_file_path :"+packageFilePath);
            try {

                PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(packageFilePath, 0);

                apkParser = new ApkParser(new File(packageFilePath));

                packageLabel = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                packageName = packageInfo.packageName;

                Intent resultIntent = new Intent(getApplicationContext(), AppProcessActivity.class);
                resultIntent.putExtra("from_notification",true);
                resultIntent.putExtra("package_name",packageName);
                resultIntent.putExtra("package_label",packageLabel);
                resultIntent.putExtra("package_file_path",packageFilePath);

                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity( this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                processNotify.updateIntent(resultPendingIntent);

            } catch (Exception e) {
                Ln.e(e);
                broadcastStatus("exit_process_on_error");
            }

            sourceOutputDir = Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+ packageName;
            javaSourceOutputDir = sourceOutputDir + "/java";

            Ln.d(sourceOutputDir);

            SourceInfo.initialise(this);

            exceptionHandler = new ExceptionHandler(getApplicationContext(), javaSourceOutputDir, packageName);
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
            Processor.extract(this);
        }
    }

    public void publishProgress(String progressText){
        switch (progressText) {
            case "start_activity": {
                broadcastStatusWithPackageInfo(progressText, javaSourceOutputDir + "/", packageName);
                break;
            }
            case "start_activity_with_error": {
                broadcastStatusWithPackageInfo(progressText, javaSourceOutputDir + "/", packageName);
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
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, status);
        Ln.d("Sending Intent "+status);
        sendBroadcast(localIntent);
    }
    public void broadcastStatus(String statusKey, String statusData){
        sendNotification(statusKey,statusData);
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                                    .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                                    .putExtra(Constants.PROCESS_STATUS_MESSAGE, statusData);
        Ln.d("Sending Intent "+statusKey);
        sendBroadcast(localIntent);
    }
    public void broadcastStatusWithPackageInfo(String statusKey, String dir, String packId){
        sendNotification(statusKey,"");
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                .putExtra(Constants.PROCESS_DIR, dir)
                .putExtra(Constants.PROCESS_PACKAGE_ID, packId);
        Ln.d("Sending Intent "+statusKey);
        sendBroadcast(localIntent);
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
            case "xml":
                processNotify.updateTitleText("Extracting XML Resources", "");
                break;
            case "exit":
                processNotify.cancel();
                break;

            default:
                processNotify.updateText(statusData);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            processNotify.cancel();
        } catch (Exception e){
            Ln.e(e);
        }
    }
}
