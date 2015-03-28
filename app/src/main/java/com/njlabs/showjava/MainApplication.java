package com.njlabs.showjava;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.njlabs.showjava.utils.FontsOverride;

public class MainApplication extends Application {
	
    public void onCreate() 
    {
        super.onCreate();
        Crashlytics.start(this);
        FontsOverride.setDefaultFont(this);
    }
}
