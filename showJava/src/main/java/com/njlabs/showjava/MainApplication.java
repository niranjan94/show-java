package com.njlabs.showjava;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formKey = "",
        formUri = "https://njlabs.cloudant.com/acra-showjava/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="octrappletherwareedidpea",
        formUriBasicAuthPassword="mA33cYSeE1gTAtnohQEO4BLG",
        mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = false, // optional, default false
        resToastText = R.string.crash_toast_text
        )
public class MainApplication extends Application {
	
    public void onCreate() 
    {
        super.onCreate();
        ACRA.init(this);
        FontsOverride.setDefaultFont(this);
    }
}
