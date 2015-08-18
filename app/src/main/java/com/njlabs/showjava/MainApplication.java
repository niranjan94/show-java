package com.njlabs.showjava;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.njlabs.showjava.utils.FontsOverride;

import io.fabric.sdk.android.Fabric;
import ollie.Ollie;

public class MainApplication extends Application {
	
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        FontsOverride.with(this);

        Ollie.with(this)
                .setName("database.db")
                .setVersion(1)
                .setLogLevel(Ollie.LogLevel.FULL)
                .setCacheSize(8 * 1024 * 1024)
                .init();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
