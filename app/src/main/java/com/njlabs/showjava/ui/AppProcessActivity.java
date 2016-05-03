package com.njlabs.showjava.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.njlabs.showjava.Constants;
import com.njlabs.showjava.R;
import com.njlabs.showjava.processor.ProcessService;
import com.njlabs.showjava.utils.Utils;
import com.njlabs.showjava.utils.logging.Ln;

import net.dongliu.apk.parser.ApkParser;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URI;

public class AppProcessActivity extends BaseActivity {

    private TextView CurrentStatus;
    private TextView CurrentLine;
    private String packageFilePath;
    private BroadcastReceiver processStatusReceiver;
    private String decompilerToUse = "cfr";

    private boolean processStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processStatusReceiver = new ProcessStatus();

        setupLayoutNoActionBar(R.layout.activity_progress);

        CurrentStatus = (TextView) findViewById(R.id.current_status);
        CurrentLine = (TextView) findViewById(R.id.current_line);

        TextView appNameView = (TextView) findViewById(R.id.current_package_name);

        CurrentStatus.setText(R.string.status_starting_decompiler);

        if (getIntent().getDataString() == null || getIntent().getDataString().equals("")) {

            Bundle extras = getIntent().getExtras();

            appNameView.setText(extras.getString("package_label"));
            packageFilePath = extras.getString("package_file_path");

            if(packageFilePath != null ){
                try {
                    ApkParser apkParser = new ApkParser(new File(packageFilePath));
                    appNameView.setText(apkParser.getApkMeta().getLabel());
                } catch (Exception e) {
                    Ln.e(e);
                    exitWithError();
                }

                if(extras.containsKey("decompiler")){
                    decompilerToUse = extras.getString("decompiler");
                }
            } else {
                finish();
            }
        } else {
            packageFilePath = (new File(URI.create(getIntent().getDataString()))).getAbsolutePath();
            if (FilenameUtils.isExtension(packageFilePath, "apk")) {
                try {
                    ApkParser apkParser = new ApkParser(new File(packageFilePath));
                    appNameView.setText(apkParser.getApkMeta().getLabel());
                } catch (Exception e) {
                    Ln.e(e);
                    exitWithError();
                }
            }
        }

        if(fromNotification()&&Utils.isProcessorServiceRunning(this)){
            CurrentStatus.setText(getResources().getString(R.string.status_processing));
            CurrentLine.setText("");
        } else {
            startProcessorService();
        }

        appNameView.setSingleLine(false);
        appNameView.setEllipsize(TextUtils.TruncateAt.END);
        appNameView.setLines(1);

        CurrentStatus.setSingleLine(false);
        CurrentStatus.setEllipsize(TextUtils.TruncateAt.END);
        CurrentStatus.setLines(1);

        setupGears();

        registerBroadcastReceiver();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!fromNotification()) {
                    if(!processStarted || Utils.isProcessorServiceRunning(baseContext)) {
                        try {
                            unregisterReceiver(processStatusReceiver);
                        } catch (Exception ignored) {

                        }
                        Utils.forceKillAllProcessorServices(baseContext);
                        final Intent mainIntent = new Intent(baseContext, ErrorActivity.class);
                        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mainIntent);
                        finish();
                    }
                }
            }
        }, 5000);
    }

    private void setupGears(){
        final ImageView GearProgressLeft = (ImageView) findViewById(R.id.gear_progress_left);
        final ImageView GearProgressRight = (ImageView) findViewById(R.id.gear_progress_right);

        final RotateAnimation GearProgressLeftAnim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        GearProgressLeftAnim.setRepeatCount(Animation.INFINITE);
        GearProgressLeftAnim.setDuration((long) 2 * 1500);
        GearProgressLeftAnim.setInterpolator(new LinearInterpolator());

        final RotateAnimation GearProgressRightAnim = new RotateAnimation(360.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        GearProgressRightAnim.setRepeatCount(Animation.INFINITE);
        GearProgressRightAnim.setDuration((long) 1500);
        GearProgressRightAnim.setInterpolator(new LinearInterpolator());

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
    }

    private void startProcessorService() {
        Utils.killAllProcessorServices(this, true);
        Intent mServiceIntent = new Intent(getContext(), ProcessService.class);
        mServiceIntent.setAction(Constants.ACTION.START_PROCESS);
        mServiceIntent.putExtra("package_file_path", packageFilePath);
        mServiceIntent.putExtra("decompiler", decompilerToUse);
        startService(mServiceIntent);
    }

    private void registerBroadcastReceiver() {
        IntentFilter statusIntentFilter = new IntentFilter(Constants.PROCESS_BROADCAST_ACTION);
        registerReceiver(processStatusReceiver, statusIntentFilter);
    }

    @SuppressWarnings("unused")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra("result");
                finish();
            }
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else {
                finish();
            }
        }
    }

    private boolean fromNotification() {
        return getIntent().hasExtra("from_notification") && getIntent().getBooleanExtra("from_notification", false);
    }

    private void exitWithError() {
        Toast.makeText(baseContext, R.string.decompiler_initialise_error, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(processStatusReceiver);
        } catch (Exception ignored) {

        }
    }

    private class ProcessStatus extends BroadcastReceiver {

        private ProcessStatus() {

        }

        public void onReceive(Context context, Intent intent) {
            String statusKey = "";
            String statusData = "";
            if (intent.hasExtra(Constants.PROCESS_STATUS_KEY)) {
                statusKey = intent.getStringExtra(Constants.PROCESS_STATUS_KEY);
            }
            if (intent.hasExtra(Constants.PROCESS_STATUS_MESSAGE)) {
                statusData = intent.getStringExtra(Constants.PROCESS_STATUS_MESSAGE);
            }
            switch (statusKey) {
                case "optimise_dex_start":
                    processStarted = true;
                    CurrentStatus.setText(R.string.status_optimising_dex);
                    break;

                case "optimising":
                    processStarted = true;
                    CurrentStatus.setText(R.string.status_optimising_dex);
                    CurrentLine.setText("");
                    break;

                case "optimise_dex_finish":
                    CurrentStatus.setText(R.string.status_optimising_dex_finish);
                    break;

                case "merging_classes":
                    CurrentStatus.setText(R.string.status_merging_classes);
                    CurrentLine.setText("");
                    break;

                case "start_activity":
                    if (intent.getStringExtra(Constants.PROCESS_DIR) != null && intent.getStringExtra(Constants.PROCESS_PACKAGE_ID) != null) {
                        Intent iOne = new Intent(getApplicationContext(), JavaExplorer.class);
                        iOne.putExtra("java_source_dir", intent.getStringExtra(Constants.PROCESS_DIR));
                        iOne.putExtra("package_id", intent.getStringExtra(Constants.PROCESS_PACKAGE_ID));
                        startActivityForResult(iOne, 1);
                    }
                    break;

                case "start_activity_with_error":
                    Toast.makeText(baseContext, R.string.incomplete_source, Toast.LENGTH_SHORT).show();
                    if (intent.getStringExtra(Constants.PROCESS_DIR) != null && intent.getStringExtra(Constants.PROCESS_PACKAGE_ID) != null) {
                        Intent iTwo = new Intent(getApplicationContext(), JavaExplorer.class);
                        iTwo.putExtra("java_source_dir", intent.getStringExtra(Constants.PROCESS_DIR));
                        iTwo.putExtra("package_id", intent.getStringExtra(Constants.PROCESS_PACKAGE_ID));
                        startActivityForResult(iTwo, 1);
                    }
                    break;

                case "exit_process_on_error":
                    Toast.makeText(baseContext, R.string.error_exiting, Toast.LENGTH_SHORT).show();
                    finish();
                    break;

                case "finaldex":
                    CurrentStatus.setText(R.string.status_optimising_dex_finish);
                    CurrentLine.setText("");
                    break;

                case "dex2jar":
                    CurrentStatus.setText(R.string.status_dex2jar);
                    break;

                case "jar2java":
                    CurrentStatus.setText(R.string.status_jar2java);
                    break;

                case "res":
                    CurrentStatus.setText(R.string.status_extracting_res);
                    break;

                case "exit":
                    finish();
                    break;

                default:
                    CurrentLine.setText(statusData);
            }
        }
    }
}