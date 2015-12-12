package com.njlabs.showjava.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.njlabs.showjava.R;
import com.njlabs.showjava.utils.AesCbcWithIntegrity;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

@SuppressWarnings("unused")
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    public Context baseContext;
    public Toolbar toolbar;
    protected SharedPreferences prefs;
    private AdView mAdView;
    public boolean isPro = false;
    public boolean hawkLoaded;
    private String androidID;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseContext = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        androidID =  Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        isPro = get();
    }

    public void setupLayout(int layoutRef) {
        setContentView(layoutRef);
        setupToolbar(null);
        setupGoogleAds();
    }

    public void setupLayout(int layoutRef, String title) {
        setContentView(layoutRef);
        setupToolbar(title);
        setupGoogleAds();
    }

    public void setupLayoutNoActionBar(int layoutRef) {
        setContentView(layoutRef);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(String title) {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (title != null) {
            getSupportActionBar().setTitle(title);
        } else {
            if(isPro()) {
                ActivityInfo activityInfo;
                try {
                    activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                    String currentTitle = activityInfo.loadLabel(getPackageManager()).toString();
                    if(currentTitle.trim().equals("Show Java")){
                        getSupportActionBar().setTitle("Show Java Pro");
                    }
                } catch (PackageManager.NameNotFoundException ignored) {

                }
            }
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.about_option:
                startActivity(new Intent(getBaseContext(), About.class));
                return true;

            case R.id.bug_report_option:
                Uri uri = Uri.parse("https://github.com/niranjan94/show-java/issues/new");
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                return true;

            case R.id.settings_option:
                startActivity(new Intent(getBaseContext(), SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Context getContext() {
        return baseContext;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean checkDataConnection() {
        boolean status = false;
        ConnectivityManager connectivityMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityMgr.getActiveNetworkInfo() != null &&
                connectivityMgr.getActiveNetworkInfo().isAvailable() &&
                connectivityMgr.getActiveNetworkInfo().isConnected()) {
            status = true;
        }
        return status;
    }

    public boolean isPro(){
        return isPro;
    }
    
    public boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private void setupGoogleAds() {
        mAdView = (AdView) findViewById(R.id.adView);
        if (mAdView != null) {
            mAdView.setVisibility(View.GONE);
            if (!isPro()) {
                AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
                mAdView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        super.onAdFailedToLoad(errorCode);
                        mAdView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        mAdView.setVisibility(View.VISIBLE);
                    }
                });
                mAdView.loadAd(adRequest);
                if (!checkDataConnection()) {
                    mAdView.setVisibility(View.GONE);
                }
            }
        }
    }

    public void put(boolean val){
        try {
            AesCbcWithIntegrity.SecretKeys keys = new AesCbcWithIntegrity.SecretKeys(getResources().getString(R.string.cc),getResources().getString(R.string.ii));
            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac;
            cipherTextIvMac = AesCbcWithIntegrity.encrypt(val ? "true" : "false", keys);
            String ciphertextString = cipherTextIvMac.toString();
            prefs.edit().putString(androidID,ciphertextString ).apply();
        } catch (UnsupportedEncodingException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public boolean get(){
        try {
            AesCbcWithIntegrity.SecretKeys keys = new AesCbcWithIntegrity.SecretKeys(getResources().getString(R.string.cc),getResources().getString(R.string.ii));
            String plainText = AesCbcWithIntegrity.decryptString(prefs.getString(androidID,""), keys);
            return (plainText.equals("true"));
        } catch (Exception e) {
            return false;
        }
    }


}
