package com.njlabs.showjava.processor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.njlabs.showjava.Constants;
import com.njlabs.showjava.R;
import com.njlabs.showjava.ui.AppProcessActivity;
import com.njlabs.showjava.ui.JavaExplorer;
import com.njlabs.showjava.utils.ExceptionHandler;
import com.njlabs.showjava.utils.Notify;
import com.njlabs.showjava.utils.SourceInfo;
import com.njlabs.showjava.utils.Utils;
import com.njlabs.showjava.utils.logging.Ln;

import net.dongliu.apk.parser.ApkParser;

import java.io.File;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
public class ProcessService extends Service {

    public String packageName;
    public String packageFilePath;
    public String packageLabel;

    public String javaSourceOutputDir;
    public String sourceOutputDir;
    public ExceptionHandler exceptionHandler;

    public Handler UIHandler;

    private Notify processNotify;
    public ApkParser apkParser;

    public String decompilerToUse = "cfr";

    private int startID;

    public void onCreate() {
        super.onCreate();
    }

    public int STACK_SIZE;
    public boolean IGNORE_LIBS;

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        this.startID = startId;
        /**
         * Initialize a handler for posting runnables that have to run on the UI thread
         */
        UIHandler = new Handler();

        /**
         * Receive action from the intent and decide whether to start or stop the existing process
         */
        if (intent.getAction().equals(Constants.ACTION.START_PROCESS)) {

            /**
             * The intent's actions is {@link Constants.ACTION.START_PROCESS}
             * Which means, the process has to start.
             *
             * We build the notification and start the process as a foreground process (to prevent it
             * from being killed on exit)
             */
            startForeground(Constants.PROCESS_NOTIFICATION_ID, buildNotification());
            handleIntent(intent);

        } else if (intent.getAction().equals(Constants.ACTION.STOP_PROCESS)) {

            /**
             * The intent's actions is {@link Constants.ACTION.STOP_PROCESS}
             * Which means, the process has to stop and kill itself.
             *
             * We are broadcasting an 'exit' status so that any activity listening can exit.
             * We stop the foreground process.
             * And we forcefully kill the service.
             *
             * Uses the {@link #killSelf()} method.
             */

            int toKillStartId = intent.getIntExtra("startId",-1);
            killSelf(true, toKillStartId);

        } else if(intent.getAction().equals(Constants.ACTION.STOP_PROCESS_FOR_NEW)) {
            killSelf(false, -1);
        }

        return START_NOT_STICKY;
    }

    private void handleIntent(Intent workIntent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        STACK_SIZE = Integer.valueOf(prefs.getString("thread_stack_size", String.valueOf(20 * 1024 * 1024)));
        IGNORE_LIBS = prefs.getBoolean("ignore_libraries", true);

        /**
         * This is the main starting point of the ProcessorService. The intent is read and handled here
         */
        Bundle extras = workIntent.getExtras();
        if (extras != null) {

            if(extras.containsKey("decompiler")){
                decompilerToUse = extras.getString("decompiler");
            }

            packageFilePath = extras.getString("package_file_path");
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(packageFilePath, 0);
                        apkParser = new ApkParser(new File(packageFilePath));

                        packageLabel = apkParser.getApkMeta().getLabel();
                        packageName = packageInfo.packageName;

                    } catch (Exception e) {
                        Ln.e(e);
                        broadcastStatus("exit_process_on_error");
                    }
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent resultIntent = new Intent(getApplicationContext(), AppProcessActivity.class);
                            resultIntent.putExtra("from_notification", true);
                            resultIntent.putExtra("package_name", packageName);
                            resultIntent.putExtra("package_label", packageLabel);
                            resultIntent.putExtra("package_file_path", packageFilePath);
                            resultIntent.putExtra("decompiler", decompilerToUse);

                            PendingIntent resultPendingIntent =
                                    PendingIntent.getActivity(ProcessService.this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                            processNotify.updateIntent(resultPendingIntent);
                        }
                    });
                    sourceOutputDir = Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + packageName;
                    javaSourceOutputDir = sourceOutputDir + "/java";

                    Ln.d(sourceOutputDir);

                    SourceInfo.initialise(ProcessService.this);

                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            exceptionHandler = new ExceptionHandler(getApplicationContext(), javaSourceOutputDir, packageName);
                            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
                            Processor.extract(ProcessService.this);
                        }
                    });
                }
            })).start();
        } else {
            killSelf(true, startID);
        }
    }

    public void publishProgress(String progressText) {
        switch (progressText) {
            case "start_activity": {
                decompileDone();
                broadcastStatusWithPackageInfo(progressText, sourceOutputDir, packageName);
                kill();
                break;
            }
            case "start_activity_with_error": {
                decompileDone();
                broadcastStatusWithPackageInfo(progressText, sourceOutputDir, packageName);
                UIHandler.post(new ToastRunnable("Decompilation completed with errors. This incident has been reported to the developer."));
                kill();
                break;
            }
            case "exit_process_on_error":
                broadcastStatus(progressText);
                UIHandler.post(new ToastRunnable("The app you selected cannot be decompiled. Please select another app."));
                kill();
                break;
            default:
                break;
        }
    }

    private void decompileDone() {
        showCompletedNotification();
    }

    private void showCompletedNotification() {

        Intent resultIntent = new Intent(getApplicationContext(), JavaExplorer.class);
        resultIntent.putExtra("from_notification", true);
        resultIntent.putExtra("java_source_dir", sourceOutputDir);
        resultIntent.putExtra("package_id", packageName);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(ProcessService.this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(packageLabel + " has been decompiled.")
                .setContentText("Tap to browse source")
                .setSmallIcon(R.drawable.stat_action_done)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        mNotifyManager.notify(2, mBuilder.build());
    }

    public void broadcastStatus(String status) {
        sendNotification(status, "");
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, status);
        sendBroadcast(localIntent);
    }

    public void broadcastStatus(String statusKey, String statusData) {
        sendNotification(statusKey, statusData);
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                .putExtra(Constants.PROCESS_STATUS_MESSAGE, statusData);
        sendBroadcast(localIntent);
    }

    public void broadcastStatusWithPackageInfo(String statusKey, String dir, String packId) {
        sendNotification(statusKey, "");
        Intent localIntent = new Intent(Constants.PROCESS_BROADCAST_ACTION)
                .putExtra(Constants.PROCESS_STATUS_KEY, statusKey)
                .putExtra(Constants.PROCESS_DIR, dir)
                .putExtra(Constants.PROCESS_PACKAGE_ID, packId);
        sendBroadcast(localIntent);
    }

    private void sendNotification(String statusKey, String statusData) {
        switch (statusKey) {
            case "optimise_dex_start":
                processNotify.updateTitleText("Optimising dex file", "Processing ...");
                break;
            case "optimising":
                processNotify.updateTitleText("Optimising dex file", "Processing ...");
                break;
            case "optimise_dex_finish":
                processNotify.updateTitleText("Finishing optimisation", "Processing ...");
                break;
            case "merging_classes":
                processNotify.updateTitleText("Merging classes", "Processing ...");
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
                processNotify.updateTitleText("Finishing optimisation", "Processing ...");
                break;
            case "dex2jar":
                processNotify.updateTitleText("Decompiling dex to jar", "Processing ...");
                break;
            case "jar2java":
                processNotify.updateTitleText("Decompiling to java", "Processing ...");
                break;
            case "res":
                processNotify.updateTitleText("Extracting Resources", "Processing ...");
                break;
            case "exit":
                try {
                    processNotify.cancel();
                } catch (Exception e) {
                    Ln.i(e);
                }
                break;

            default:
                processNotify.updateText(statusData);
        }
    }

    private Notification buildNotification() {

        Intent stopIntent = new Intent(this, ProcessService.class);
        stopIntent.setAction(Constants.ACTION.STOP_PROCESS);
        stopIntent.putExtra("startID", startID);

        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Decompiling")
                .setContentText("Processing the apk")
                .setSmallIcon(R.drawable.stat_action_running)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .addAction(R.drawable.ic_action_kill, "Stop decompiler", pendingStopIntent)
                .setAutoCancel(false);

        mBuilder.setProgress(0, 0, true);

        Notification notification = mBuilder.build();

        mNotifyManager.notify(Constants.PROCESS_NOTIFICATION_ID, notification);

        processNotify = new Notify(mNotifyManager, mBuilder, Constants.PROCESS_NOTIFICATION_ID);

        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            processNotify.cancel();
        } catch (Exception e) {
            Ln.e(e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void kill() {
        stopForeground(true);
        stopSelf();
    }

    private class ToastRunnable implements Runnable {

        final String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }
    }

    private void killSelf(boolean shouldBroadcast, int startID){
        if(shouldBroadcast){
            broadcastStatus("exit");
        }
        stopForeground(true);
        stopSelf(startID);
        try {
            NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotifyManager.cancel(Constants.PROCESS_NOTIFICATION_ID);
            Utils.forceKillAllProcessorServices(this);
        } catch (Exception e) {
            Ln.e(e);
        }
    }
}
