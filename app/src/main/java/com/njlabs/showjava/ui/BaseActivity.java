package com.njlabs.showjava.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.njlabs.showjava.R;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    public Context baseContext;
    public Toolbar toolbar;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseContext = this;

        mAdView = (AdView) findViewById(R.id.adView);

        if(mAdView != null){
            mAdView.setVisibility(View.GONE);
            mAdView.setAdSize(AdSize.SMART_BANNER);
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
            if(!checkDataConnection()){
                mAdView.setVisibility(View.GONE);
            }
        }
    }
    public void setupLayout(int layoutRef){
        setContentView(layoutRef);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    public void setupLayout(int layoutRef, String title){
        setContentView(layoutRef);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
    }
    public void setupLayoutNoActionBar(int layoutRef){
        setContentView(layoutRef);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.about_option) {
                        return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public Context getContext(){
        return baseContext;
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

    private boolean checkDataConnection(){
        boolean status = false;
        ConnectivityManager connectivityMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityMgr.getActiveNetworkInfo()!=null &&
                connectivityMgr.getActiveNetworkInfo().isAvailable() &&
                connectivityMgr.getActiveNetworkInfo().isConnected()) {
            status = true;
        }
        return status;
    }
}
