package com.njlabs.showjava;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

public class MainApplication extends Application {
	
    public void onCreate() 
    {
        super.onCreate();
        Crashlytics.start(this);
        FontsOverride.setDefaultFont(this);
    }
}
