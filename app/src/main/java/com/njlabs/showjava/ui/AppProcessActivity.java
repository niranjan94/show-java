package com.njlabs.showjava.ui;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.njlabs.showjava.Constants;
import com.njlabs.showjava.R;
import com.njlabs.showjava.processor.ProcessService;
import com.njlabs.showjava.utils.Notify;
import com.njlabs.showjava.utils.logging.Ln;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URI;

public class AppProcessActivity extends BaseActivity {
	
	private TextView CurrentStatus;
	private TextView CurrentLine;
	private String PackageId;
	private String PackageDir;
	private String PackageName;

    private Notify processNotify;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

        Ln.d("onCreate AppProcessActivity");
        setupLayoutNoActionBar(R.layout.activity_progress);
        registerBroadcastReceiver();
        if(isProcessorRunning()){
            CurrentStatus.setText("Resuming Decompiler");
        } else {

            if((getIntent().getDataString()!=null&&getIntent().getDataString().equals(""))||getIntent().getDataString()==null)
            {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    PackageName = extras.getString("package_name");
                    PackageId = extras.getString("package_id");
                    PackageDir = extras.getString("package_dir");
                }
            }
            else
            {
                PackageDir = (new File(URI.create(getIntent().getDataString()))).getAbsolutePath();
                if(FilenameUtils.isExtension(PackageDir, "apk"))
                {
                    PackageManager pm = getPackageManager();
                    PackageInfo info = pm.getPackageArchiveInfo(PackageDir, PackageManager.GET_ACTIVITIES);
                    if(info != null){
                        ApplicationInfo appInfo = info.applicationInfo;

                        if(Build.VERSION.SDK_INT >= 8){
                            appInfo.sourceDir = PackageDir;
                            appInfo.publicSourceDir = PackageDir;
                        }
                    }
                    try {
                        if (info != null) {
                            PackageName = info.applicationInfo.loadLabel(getPackageManager()).toString();
                            PackageId = info.packageName;
                        }
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "The app you selected cannot be decompiled. Please select another app.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
                else
                {
                    PackageName=FilenameUtils.getName(PackageDir);
                    PackageId=FilenameUtils.getName(PackageDir).replaceAll(" ", "_").toLowerCase();
                }
            }
        }

        TextView AppName=(TextView) findViewById(R.id.current_package_name);
        AppName.setSingleLine(false);
        AppName.setEllipsize(TextUtils.TruncateAt.END);
        AppName.setLines(1);

        CurrentStatus=(TextView) findViewById(R.id.current_status);
        CurrentLine=(TextView) findViewById(R.id.current_line);
        

        CurrentStatus.setSingleLine(false);
        CurrentStatus.setEllipsize(TextUtils.TruncateAt.END);
        CurrentStatus.setLines(1);
        
        CurrentLine.setSingleLine(false);
        CurrentLine.setEllipsize(TextUtils.TruncateAt.END);
        CurrentLine.setLines(1);
        
        final ImageView GearProgressLeft = (ImageView) findViewById(R.id.gear_progress_left);
        final ImageView GearProgressRight = (ImageView) findViewById(R.id.gear_progress_right);
        
        final RotateAnimation GearProgressLeftAnim = new RotateAnimation(0.0f,360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        GearProgressLeftAnim.setRepeatCount(Animation.INFINITE);
        GearProgressLeftAnim.setDuration((long) 2*1500);
        GearProgressLeftAnim.setInterpolator(this,android.R.anim.linear_interpolator);
        
        final RotateAnimation GearProgressRightAnim = new RotateAnimation(360.0f,0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        GearProgressRightAnim.setRepeatCount(Animation.INFINITE);
        GearProgressRightAnim.setDuration((long) 1500);
        GearProgressRightAnim.setInterpolator(this,android.R.anim.linear_interpolator);
                
        GearProgressLeft.post(new Runnable() {   
            @Override
            public void run() {
            	GearProgressLeft.setAnimation(GearProgressLeftAnim);
            }
        });
        GearProgressLeft.post(new Runnable() {   
            @Override
            public void run() {
            	GearProgressRight.setAnimation(GearProgressRightAnim);
            }
        });

        if(!isProcessorRunning()){
            CurrentStatus.setText("Starting Decompiler");
            startProcessorService();
        }
	}

    public void startProcessorService(){

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(this, AppProcessActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity( this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Decompiling")
                .setContentText("Processing the apk")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(baseContext.getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(resultPendingIntent);

        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(Constants.PROCESS_NOTIFICATION_ID, mBuilder.build());

        processNotify = new Notify(mNotifyManager,mBuilder,Constants.PROCESS_NOTIFICATION_ID);

        Ln.d("startProcessorService AppProcessActivity");
        Intent mServiceIntent = new Intent(getContext(), ProcessService.class);
        mServiceIntent.putExtra("package_name",PackageName);
        mServiceIntent.putExtra("package_id",PackageId);
        mServiceIntent.putExtra("package_dir",PackageDir);
        startService(mServiceIntent);
    }

    public void registerBroadcastReceiver(){
        ProcessStatus processStatusReceiver = new ProcessStatus();
        IntentFilter statusIntentFilter = new IntentFilter(Constants.PROCESS_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(processStatusReceiver,statusIntentFilter);
    }

    private class ProcessStatus extends BroadcastReceiver
    {
        private ProcessStatus() {
        }
        public void onReceive(Context context, Intent intent) {
            String statusKey = "";
            String statusData = "";
            if(intent.hasExtra(Constants.PROCESS_STATUS_KEY)){
                statusKey = intent.getStringExtra(Constants.PROCESS_STATUS_KEY);
            }
            if(intent.hasExtra(Constants.PROCESS_STATUS_MESSAGE)){
                statusData = intent.getStringExtra(Constants.PROCESS_STATUS_MESSAGE);
            }
            switch(statusKey){
                case "optimise_dex_start":
                    processNotify.updateTitleText("Optimising dex file", "");
                    CurrentStatus.setText("Optimising dex file");
                    break;
                case "optimising":
                    processNotify.updateTitleText("Optimising dex file",statusData);
                    CurrentStatus.setText("Optimising dex file");
                    CurrentLine.setText(statusData);
                    break;
                case "optimise_dex_finish":
                    processNotify.updateTitleText("Finishing optimisation", "");
                    CurrentStatus.setText("Finishing optimisation");
                    break;
                case "merging_classes":
                    processNotify.updateTitleText("Merging classes","");
                    CurrentStatus.setText("Merging classes");
                    break;

                case "start_activity":
                    processNotify.cancel();
                    Intent iOne = new Intent(getApplicationContext(), JavaExplorer.class);
                    iOne.putExtra("java_source_dir",intent.getStringExtra(Constants.PROCESS_DIR));
                    iOne.putExtra("package_id", intent.getStringExtra(Constants.PROCESS_PACKAGE_ID));
                    startActivityForResult(iOne, 1);
                    break;

                case "start_activity_with_error":
                    processNotify.cancel();
                    Intent iTwo = new Intent(getApplicationContext(), JavaExplorer.class);
                    iTwo.putExtra("java_source_dir",intent.getStringExtra(Constants.PROCESS_DIR));
                    iTwo.putExtra("package_id", intent.getStringExtra(Constants.PROCESS_PACKAGE_ID));
                    startActivityForResult(iTwo, 1);
                    break;

                case "exit_process_on_error":
                    processNotify.cancel();
                    finish();
                    break;

                case "finaldex":
                    processNotify.updateTitleText("Finishing optimisation","");
                    CurrentStatus.setText("Finishing optimisation");
                    CurrentLine.setText("");
                    break;

                case "dex2jar":
                    processNotify.updateTitleText("Decompiling dex to jar","");
                    CurrentStatus.setText("Decompiling dex to jar");
                    break;

                case "jar2java":
                    processNotify.updateTitleText("Decompiling to java","");
                    CurrentStatus.setText("Decompiling to java");
                    break;
                case "exit":
                    processNotify.cancel();
                    finish();
                    break;
                default:
                    processNotify.updateText(statusData);
                    CurrentLine.setText(statusData);
            }
        }
    }
	@SuppressWarnings("unused")
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		  if (requestCode == 1) {
		     if(resultCode == RESULT_OK) {
		         String result=data.getStringExtra("result");
		         finish();
		     }
		     if (resultCode == RESULT_CANCELED) {
		    	 finish();
		     } else {
		    	 finish();
		     }		     
		  }
	}

    private boolean isProcessorRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.njlabs.showjava.processor.ProcessService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}