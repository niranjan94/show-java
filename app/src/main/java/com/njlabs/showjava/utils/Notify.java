package com.njlabs.showjava.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Niranjan on 24-05-2015.
 */
@SuppressWarnings("unused")
public class Notify {

    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    int NOTIFICATION_ID;
    long time = 0;

    public Notify(NotificationManager mNotifyManager, NotificationCompat.Builder mBuilder, int NOTIFICATION_ID) {
        this.mNotifyManager = mNotifyManager;
        this.mBuilder = mBuilder;
        this.NOTIFICATION_ID = NOTIFICATION_ID;
    }

    public void updateTitle(String title) {
        mBuilder.setContentTitle(title);
        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void updateText(String text) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - time >= 500) {
            mBuilder.setContentText(text);
            mBuilder.setProgress(0, 0, true);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            time = currentTime;
        }
    }

    public void updateTitleText(String title, String text) {
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(text);
        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void updateIntent(PendingIntent pendingIntent) {
        mBuilder.setContentIntent(pendingIntent);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void cancel() {
        mNotifyManager.cancel(NOTIFICATION_ID);
    }
}
