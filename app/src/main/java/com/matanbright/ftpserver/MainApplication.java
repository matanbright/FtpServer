package com.matanbright.ftpserver;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.preference.PreferenceManager;


public class MainApplication extends Application {

    public static final String NOTIFICATION_CHANNEL_ID__SERVER_STATUS = "server_status";
    private static final String NOTIFICATION_CHANNEL_NAME__SERVER_STATUS = "Server Status";

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationChannel notificationChannel = new NotificationChannel(
            NOTIFICATION_CHANNEL_ID__SERVER_STATUS,
            NOTIFICATION_CHANNEL_NAME__SERVER_STATUS,
            NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}
