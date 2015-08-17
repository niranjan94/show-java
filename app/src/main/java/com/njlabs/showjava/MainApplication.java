package com.njlabs.showjava;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.njlabs.showjava.utils.FontsOverride;

import io.fabric.sdk.android.Fabric;

public class MainApplication extends Application {
	
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        FontsOverride.with(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
