package com.njlabs.showjava.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processStatusReceiver = new ProcessStatus();

        setupLayoutNoActionBar(R.layout.activity_progress);

        CurrentStatus = (TextView) findViewById(R.id.current_status);
        CurrentLine = (TextView) findViewById(R.id.current_line);

        TextView appNameView = (TextView) findViewById(R.id.current_package_name);

        CurrentStatus.setText("Starting Decompiler");

        if (getIntent().getDataString() == null || getIntent().getDataString().equals("")) {
            appNameView.setText(getIntent().getStringExtra("package_label"));
            packageFilePath = getIntent().getStringExtra("package_file_path");

            try {
                ApkParser apkParser = new ApkParser(new File(packageFilePath));
                appNameView.setText(apkParser.getApkMeta().getLabel());
            } catch (Exception e) {
                Ln.e(e);
                exitWithError();
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

        if (!fromNotification()) {
            startProcessorService();
        } else {
            CurrentStatus.setText("Processing ...");
            CurrentLine.setText("");
        }

        appNameView.setSingleLine(false);
        appNameView.setEllipsize(TextUtils.TruncateAt.END);
        appNameView.setLines(1);

        CurrentStatus.setSingleLine(false);
        CurrentStatus.setEllipsize(TextUtils.TruncateAt.END);
        CurrentStatus.setLines(1);

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

        registerBroadcastReceiver();
    }

    public void startProcessorService() {
        Utils.killAllProcessorServices(this);
        Intent mServiceIntent = new Intent(getContext(), ProcessService.class);
        mServiceIntent.setAction(Constants.ACTION.START_PROCESS);
        mServiceIntent.putExtra("package_file_path", packageFilePath);
        startService(mServiceIntent);
    }

    public void registerBroadcastReceiver() {
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
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        }
    }

    private boolean fromNotification() {
        return getIntent().hasExtra("from_notification") && getIntent().getBooleanExtra("from_notification", false);
    }

    private void exitWithError() {
        finish();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        Toast.makeText(baseContext, "There was an error initialising the decompiler with the app you selected.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(processStatusReceiver);
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
                    CurrentStatus.setText("Optimising dex file");
                    break;
                case "optimising":
                    CurrentStatus.setText("Optimising dex file");
                    CurrentLine.setText("");
                    break;
                case "optimise_dex_finish":
                    CurrentStatus.setText("Finishing optimisation");
                    break;
                case "merging_classes":
                    CurrentStatus.setText("Merging classes");
                    CurrentLine.setText("");
                    break;

                case "start_activity":
                    if (intent.getStringExtra(Constants.PROCESS_DIR) != null && intent.getStringExtra(Constants.PROCESS_PACKAGE_ID) != null) {
                        Intent iOne = new Intent(getApplicationContext(), JavaExplorer.class);
                        iOne.putExtra("java_source_dir", intent.getStringExtra(Constants.PROCESS_DIR));
                        iOne.putExtra("package_id", intent.getStringExtra(Constants.PROCESS_PACKAGE_ID));
                        startActivityForResult(iOne, 1);
                        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    }

                    break;

                case "start_activity_with_error":
                    Toast.makeText(baseContext, "An error occurred. Generated source may be incomplete.", Toast.LENGTH_SHORT).show();
                    if (intent.getStringExtra(Constants.PROCESS_DIR) != null && intent.getStringExtra(Constants.PROCESS_PACKAGE_ID) != null) {
                        Intent iTwo = new Intent(getApplicationContext(), JavaExplorer.class);
                        iTwo.putExtra("java_source_dir", intent.getStringExtra(Constants.PROCESS_DIR));
                        iTwo.putExtra("package_id", intent.getStringExtra(Constants.PROCESS_PACKAGE_ID));
                        startActivityForResult(iTwo, 1);
                        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    }
                    break;

                case "exit_process_on_error":
                    finish();
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    break;

                case "finaldex":
                    CurrentStatus.setText("Finishing optimisation");
                    CurrentLine.setText("");
                    break;

                case "dex2jar":
                    CurrentStatus.setText("Decompiling dex to jar");
                    break;
                case "jar2java":
                    CurrentStatus.setText("Decompiling to java");
                    break;
                case "res":
                    CurrentStatus.setText("Extracting Resources");
                    break;
                case "exit":
                    finish();
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    break;
                default:
                    CurrentLine.setText(statusData);
            }
        }
    }
}