package com.matanbright.ftpserver.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.matanbright.ftpserver.MainApplication;
import com.matanbright.ftpserver.R;
import com.matanbright.ftpserver.activities.MainActivity;
import com.matanbright.ftpserver.objects.FtpServerWrapper;
import com.matanbright.ftpserver.utilities.NetworkUtilities;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Objects;


public class FtpServerService extends Service {

    public enum State {
        INITIALIZED,
        STARTED,
        TERMINATING
    }

    public enum FtpServerState {
        STOPPED,    // This state is temporary until the service's instance is destroyed.
        BEING_RUN,
        RUNNING,
        BEING_STOPPED
    }

    public static final String ACTION__FTP_SERVER_STATE_CHANGED = "com.matanbright.ftpserver:ftp_server_state_changed";
    public static final String ACTION__DEVICE_IP_ADDRESSES_CHANGED = "com.matanbright.ftpserver:device_ip_addresses_changed";
    private static final String WAKE_LOCK__FTP_SERVER_WAKE_LOCK = "com.matanbright.ftpserver:ftp_server_wake_lock";
    private static final int NOTIFICATION_ID__SERVER_STATUS = 10000;

    private static FtpServerService instance;

    @Nullable
    public static FtpServerService getInstance() {
        return instance;
    }

    private State state;
    private final MutableLiveData<FtpServerState> ftpServerStateMutableLiveData;
    private final MutableLiveData<ArrayList<InetAddress>> cachedDeviceIpAddressesMutableLiveData;
    private final BroadcastReceiver deviceConnectivityChangedBroadcastReceiver;
    private PowerManager.WakeLock ftpServerWakeLock;
    private FtpServerWrapper ftpServer;

    public FtpServerService() {
        super();
        state = State.INITIALIZED;
        ftpServerStateMutableLiveData = new MutableLiveData<>(FtpServerState.STOPPED);
        cachedDeviceIpAddressesMutableLiveData = new MutableLiveData<>(new ArrayList<>());
        deviceConnectivityChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateCachedDeviceIpAddresses();
            }
        };
        updateCachedDeviceIpAddresses();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        ftpServerWakeLock = getSystemService(PowerManager.class).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK__FTP_SERVER_WAKE_LOCK
        );
        ftpServerStateMutableLiveData.observeForever(ftpServerState -> broadcastFtpServerStateChanged());
        cachedDeviceIpAddressesMutableLiveData.observeForever(ftpServerState -> broadcastDeviceIpAddressesChanged());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(deviceConnectivityChangedBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(deviceConnectivityChangedBroadcastReceiver);
        releaseFtpServerWakeLock();
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (state == State.INITIALIZED) {
            state = State.STARTED;
            startForeground(NOTIFICATION_ID__SERVER_STATUS, createNotification());
            ftpServerStateMutableLiveData.setValue(FtpServerState.BEING_RUN);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            int ftpServerPort = 0;
            try {
                ftpServerPort = Integer.parseInt(sharedPreferences.getString(getString(R.string.preference_key__ftp_server_port), ""));
            } catch (Exception ignored) {}
            boolean ftpServerEnableAuthentication = sharedPreferences.getBoolean(getString(R.string.preference_key__ftp_server_enable_authentication), false);
            FtpServerWrapper.FtpUser ftpServerUser = null;
            if (ftpServerEnableAuthentication) {
                String ftpServerUserName = sharedPreferences.getString(getString(R.string.preference_key__ftp_server_user_name), "");
                String ftpServerUserPassword = sharedPreferences.getString(getString(R.string.preference_key__ftp_server_user_password), "");
                ftpServerUser = new FtpServerWrapper.FtpUser(ftpServerUserName, ftpServerUserPassword);
            }
            boolean ftpServerEnableEncryption = sharedPreferences.getBoolean(getString(R.string.preference_key__ftp_server_enable_encryption), false);
            FtpServerWrapper.FtpEncryption ftpServerEncryption = null;
            if (ftpServerEnableEncryption) {
                String ftpServerEncryptionKeystoreFilePath = sharedPreferences.getString(getString(R.string.preference_key__ftp_server_encryption_keystore_file_path), "");
                String ftpServerEncryptionKeyAlias = sharedPreferences.getString(getString(R.string.preference_key__ftp_server_encryption_key_alias), "");
                String ftpServerEncryptionKeyPassword = sharedPreferences.getString(getString(R.string.preference_key__ftp_server_encryption_key_password), "");
                boolean ftpServerForceEncryption = sharedPreferences.getBoolean(getString(R.string.preference_key__ftp_server_force_encryption), false);
                ftpServerEncryption = new FtpServerWrapper.FtpEncryption(
                    ftpServerEncryptionKeystoreFilePath,
                    ftpServerEncryptionKeyAlias,
                    ftpServerEncryptionKeyPassword,
                    ftpServerForceEncryption
                );
            }
            boolean ftpServerEnableWriting = sharedPreferences.getBoolean(getString(R.string.preference_key__ftp_server_enable_writing), false);
            try {
                ftpServer = new FtpServerWrapper(ftpServerPort, ftpServerUser, ftpServerEncryption, ftpServerEnableWriting);
                ftpServer.start();
                ftpServerStateMutableLiveData.setValue(FtpServerState.RUNNING);
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.error_message__unable_to_start_the_server), Toast.LENGTH_SHORT).show();
                terminate();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public FtpServerState getFtpServerState() {
        return ftpServerStateMutableLiveData.getValue();
    }

    public void terminate() {
        if (state == State.STARTED) {
            state = State.TERMINATING;
            ftpServerStateMutableLiveData.setValue(FtpServerState.BEING_STOPPED);
            if (ftpServer != null)
                ftpServer.stop();
            ftpServerStateMutableLiveData.setValue(FtpServerState.STOPPED);
            stopSelf();
        }
    }

    @NonNull
    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder notificationBuilder = (new Notification.Builder(this, MainApplication.NOTIFICATION_CHANNEL_ID__SERVER_STATUS))
            .setSmallIcon(R.drawable.notification_icon)
            .setColor(getColor(R.color.notification_icon_background))
            .setContentText(getString(R.string.notification_message__ftp_server_is_running))
            .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        return notification;
    }

    @SuppressLint("WakelockTimeout")
    public void acquireFtpServerWakeLock() {
        if (!ftpServerWakeLock.isHeld())
            ftpServerWakeLock.acquire();
    }

    public void releaseFtpServerWakeLock() {
        if (ftpServerWakeLock.isHeld())
            ftpServerWakeLock.release();
    }

    private void broadcastFtpServerStateChanged() {
        Intent intent = new Intent(ACTION__FTP_SERVER_STATE_CHANGED);
        sendBroadcast(intent);
    }

    private void broadcastDeviceIpAddressesChanged() {
        Intent intent = new Intent(ACTION__DEVICE_IP_ADDRESSES_CHANGED);
        sendBroadcast(intent);
    }

    private void updateCachedDeviceIpAddresses() {
        ArrayList<InetAddress> deviceIpAddresses = NetworkUtilities.getDeviceIpAddresses();
        synchronized (cachedDeviceIpAddressesMutableLiveData) {
            ArrayList<InetAddress> cachedDeviceIpAddresses =
                Objects.requireNonNull(cachedDeviceIpAddressesMutableLiveData.getValue());
            cachedDeviceIpAddresses.clear();
            cachedDeviceIpAddresses.addAll(deviceIpAddresses);
            cachedDeviceIpAddressesMutableLiveData.setValue(cachedDeviceIpAddresses);
        }
    }

    @Nullable
    public InetAddress getDeviceIpAddress() {
        synchronized (cachedDeviceIpAddressesMutableLiveData) {
            ArrayList<InetAddress> cachedDeviceIpAddresses =
                Objects.requireNonNull(cachedDeviceIpAddressesMutableLiveData.getValue());
            if (cachedDeviceIpAddresses.isEmpty())
                return null;
            return cachedDeviceIpAddresses.get(0);
        }
    }

    @NonNull
    public ArrayList<InetAddress> getDeviceIpAddresses() {
        synchronized (cachedDeviceIpAddressesMutableLiveData) {
            ArrayList<InetAddress> cachedDeviceIpAddresses =
                Objects.requireNonNull(cachedDeviceIpAddressesMutableLiveData.getValue());
            return new ArrayList<>(cachedDeviceIpAddresses);
        }
    }

    public int getFtpServerPort() {
        if (ftpServer == null)
            return 0;
        return ftpServer.getPort();
    }

    @Nullable
    public FtpServerWrapper.FtpUser getFtpServerUser() {
        if (ftpServer == null)
            return null;
        return ftpServer.getFtpUser();
    }

    @Nullable
    public FtpServerWrapper.FtpEncryption getFtpServerEncryption() {
        if (ftpServer == null)
            return null;
        return ftpServer.getFtpEncryption();
    }

    public boolean isFtpServerWritingEnabled() {
        if (ftpServer == null)
            return false;
        return ftpServer.isWritingEnabled();
    }
}
