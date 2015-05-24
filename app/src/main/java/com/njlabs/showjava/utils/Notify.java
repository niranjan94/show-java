package com.njlabs.showjava.utils;

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Niranjan on 24-05-2015.
 */
public class Notify {

    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    int NOTIFICATION_ID;

    public Notify(NotificationManager mNotifyManager, NotificationCompat.Builder mBuilder, int NOTIFICATION_ID) {
        this.mNotifyManager = mNotifyManager;
        this.mBuilder = mBuilder;
        this.NOTIFICATION_ID = NOTIFICATION_ID;
    }

    public void updateTitle(String title){
        mBuilder.setContentTitle(title);
        mNotifyManager.notify(NOTIFICATION_ID,mBuilder.build());
    }

    public void updateText(String text){
        mBuilder.setContentText(text);
        mNotifyManager.notify(NOTIFICATION_ID,mBuilder.build());
    }

    public void updateTitleText(String title, String text){
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(text);
        mNotifyManager.notify(NOTIFICATION_ID,mBuilder.build());
    }

    public void cancel(){
        mNotifyManager.cancel(NOTIFICATION_ID);
    }
}
