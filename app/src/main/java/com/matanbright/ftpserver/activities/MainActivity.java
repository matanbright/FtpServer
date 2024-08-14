package com.matanbright.ftpserver.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.matanbright.ftpserver.R;
import com.matanbright.ftpserver.activities.viewmodels.MainActivityViewModel;
import com.matanbright.ftpserver.objects.FtpServerWrapper;
import com.matanbright.ftpserver.services.FtpServerService;
import com.matanbright.ftpserver.utilities.NetworkUtilities;

import java.net.InetAddress;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE__GRANT_REQUIRED_PERMISSIONS = 10000;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private MainActivityViewModel mainActivityViewModel;
    private LinearLayout linearLayout_mainActivityContent;
    private LinearLayout linearLayout_ftpServerIndicators;
    private ImageView imageView_ftpServerUserIndicator;
    private ImageView imageView_ftpServerEncryptionIndicator;
    private ImageView imageView_ftpServerRwIndicator;
    private TextView textView_ftpServerStatus;
    private LinearLayout linearLayout_ftpServerDetails;
    private TextView textView_ftpServerEndpoint;
    private TextView textView_showMoreFtpServerEndpoints;
    private Button button_settings;
    private Button button_serverEnableOrDisable;
    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    private final BroadcastReceiver ftpServerStateChangedBroadcastReceiver;
    private final BroadcastReceiver deviceIpAddressesChangedBroadcastReceiver;

    public MainActivity() {
        super();
        onSharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (key != null) {
                if (key.equals(getString(R.string.preference_key__keep_device_awake)))
                    updateWakeLocksState();
            }
        };
        ftpServerStateChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateWakeLocksState();
                updateUi();
            }
        };
        deviceIpAddressesChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUi();
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivityViewModel = (new ViewModelProvider(this)).get(MainActivityViewModel.class);
        linearLayout_mainActivityContent = findViewById(R.id.linearLayout_mainActivityContent);
        linearLayout_ftpServerIndicators = findViewById(R.id.linearLayout_ftpServerIndicators);
        imageView_ftpServerUserIndicator = findViewById(R.id.imageView_ftpServerUserIndicator);
        imageView_ftpServerEncryptionIndicator = findViewById(R.id.imageView_ftpServerEncryptionIndicator);
        imageView_ftpServerRwIndicator = findViewById(R.id.imageView_ftpServerRwIndicator);
        textView_ftpServerStatus = findViewById(R.id.textView_ftpServerStatus);
        linearLayout_ftpServerDetails = findViewById(R.id.linearLayout_ftpServerDetails);
        textView_ftpServerEndpoint = findViewById(R.id.textView_ftpServerEndpoint);
        textView_showMoreFtpServerEndpoints = findViewById(R.id.textView_showMoreFtpServerEndpoints);
        textView_showMoreFtpServerEndpoints.setOnClickListener(this::onTextViewClick);
        button_settings = findViewById(R.id.button_settings);
        button_settings.setOnClickListener(this::onButtonClick);
        button_serverEnableOrDisable = findViewById(R.id.button_serverEnableOrDisable);
        button_serverEnableOrDisable.setOnClickListener(this::onButtonClick);
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FtpServerService.ACTION__FTP_SERVER_STATE_CHANGED);
        registerReceiver(ftpServerStateChangedBroadcastReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(FtpServerService.ACTION__DEVICE_IP_ADDRESSES_CHANGED);
        registerReceiver(deviceIpAddressesChangedBroadcastReceiver, intentFilter2);
        if (!mainActivityViewModel.havePermissionsBeenRequested()) {
            mainActivityViewModel.markThatPermissionsHaveBeenRequested();
            requestRequiredPermissions();
        }
        updateWakeLocksState();
        updateUi();
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        unregisterReceiver(ftpServerStateChangedBroadcastReceiver);
        unregisterReceiver(deviceIpAddressesChangedBroadcastReceiver);
        super.onDestroy();
    }

    private void onButtonClick(@NonNull View view) {
        int viewId = view.getId();
        if (viewId == R.id.button_settings)
            showPreferencesActivity();
        else if (viewId == R.id.button_serverEnableOrDisable) {
            FtpServerService ftpServerService = FtpServerService.getInstance();
            if (ftpServerService == null)
                startFtpServerService();
            else
                ftpServerService.terminate();
        }
    }

    private void onTextViewClick(@NonNull View view) {
        int viewId = view.getId();
        if (viewId == R.id.textView_showMoreFtpServerEndpoints)
            showFtpServerEndpointsDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE__GRANT_REQUIRED_PERMISSIONS:
                boolean permissionsWereGranted = true;
                for (int grantResult : grantResults)
                    permissionsWereGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
                if (!permissionsWereGranted) {
                    Toast.makeText(this, getString(R.string.error_message__no_permissions), Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void requestRequiredPermissions() {
        ArrayList<String> ungrantedPermissions = new ArrayList<>();
        for (String requiredPermission : REQUIRED_PERMISSIONS)
            if (checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED)
                ungrantedPermissions.add(requiredPermission);
        if (!ungrantedPermissions.isEmpty())
            requestPermissions(ungrantedPermissions.toArray(new String[0]), REQUEST_CODE__GRANT_REQUIRED_PERMISSIONS);
    }

    private void showPreferencesActivity() {
        Intent intent = new Intent(this, PreferencesActivity.class);
        startActivity(intent);
    }

    private void startFtpServerService() {
        Intent intent = new Intent(this, FtpServerService.class);
        startForegroundService(intent);
    }

    private void showFtpServerEndpointsDialog() {
        StringBuilder dialogMessageBuilder = new StringBuilder();
        FtpServerService ftpServerService = FtpServerService.getInstance();
        if (ftpServerService != null) {
            ArrayList<InetAddress> deviceIpAddresses = ftpServerService.getDeviceIpAddresses();
            int ftpServerPort = ftpServerService.getFtpServerPort();
            for (InetAddress deviceIpAddress : deviceIpAddresses) {
                String ftpServerIpEndpointString = null;
                try {
                    ftpServerIpEndpointString = NetworkUtilities.getIpEndpointString(deviceIpAddress, ftpServerPort);
                } catch (Exception ignored) {}
                if (ftpServerIpEndpointString != null) {
                    if (dialogMessageBuilder.length() > 0)
                        dialogMessageBuilder.append("\n");
                    dialogMessageBuilder.append(String.format("* %1$s", ftpServerIpEndpointString));
                }
            }
        }
        AlertDialog alertDialog = (new AlertDialog.Builder(this))
            .setTitle(getString(R.string.dialog_title__ftp_server_endpoints))
            .setMessage(dialogMessageBuilder.toString())
            .setPositiveButton(getString(R.string.button_text__close), ((dialog, which) -> dialog.dismiss()))
            .create();
        alertDialog.show();
    }

    private void updateWakeLocksState() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FtpServerService ftpServerService = FtpServerService.getInstance();
        if (ftpServerService != null) {
            ftpServerService.releaseFtpServerWakeLock();
            if (ftpServerService.getFtpServerState() != FtpServerService.FtpServerState.STOPPED) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String keepDeviceAwakeOption = sharedPreferences.getString(getString(R.string.preference_key__keep_device_awake), "");
                if (keepDeviceAwakeOption.equals(getString(R.string.keep_device_awake_option__keep_screen_on_while_server_is_running_and_app_is_open)))
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else if (keepDeviceAwakeOption.equals(getString(R.string.keep_device_awake_option__keep_cpu_on_as_long_as_server_is_running)))
                    ftpServerService.acquireFtpServerWakeLock();
            }
        }
    }

    private void updateUi() {
        FtpServerService.FtpServerState ftpServerState = FtpServerService.FtpServerState.STOPPED;
        FtpServerService ftpServerService = FtpServerService.getInstance();
        if (ftpServerService != null)
            ftpServerState = ftpServerService.getFtpServerState();

        switch (ftpServerState) {
            case STOPPED:
            case RUNNING:
                button_serverEnableOrDisable.setEnabled(true);
                button_serverEnableOrDisable.setText(getString(
                    (ftpServerState == FtpServerService.FtpServerState.STOPPED) ?
                        R.string.button_text__enable :
                        R.string.button_text__disable
                ));
                linearLayout_mainActivityContent.setBackgroundColor(getColor(
                    (ftpServerState == FtpServerService.FtpServerState.STOPPED) ?
                        R.color.background_while_server_is_not_running :
                        R.color.background_while_server_is_running
                ));
                break;
            case BEING_RUN:
            case BEING_STOPPED:
                button_serverEnableOrDisable.setEnabled(false);
                button_serverEnableOrDisable.setText(getString(
                    (ftpServerState == FtpServerService.FtpServerState.BEING_RUN) ?
                        R.string.button_text__enabling :
                        R.string.button_text__disabling
                ));
                linearLayout_mainActivityContent.setBackgroundColor(
                    getColor(R.color.background_while_server_is_changing_state)
                );
                break;
        }

        boolean ftpServerIsRunningOrBeingStopped =
            (ftpServerState == FtpServerService.FtpServerState.RUNNING ||
                ftpServerState == FtpServerService.FtpServerState.BEING_STOPPED);

        textView_ftpServerStatus.setText(getString(
            ftpServerIsRunningOrBeingStopped ?
                R.string.ftp_server_status__server_is_running :
                R.string.ftp_server_status__server_is_not_running
        ));

        linearLayout_ftpServerDetails.setVisibility(ftpServerIsRunningOrBeingStopped ? View.VISIBLE : View.INVISIBLE);
        String ftpServerEndpoint = "";
        if (ftpServerIsRunningOrBeingStopped) {
            InetAddress deviceIpAddress = ftpServerService.getDeviceIpAddress();
            if (deviceIpAddress != null) {
                int ftpServerPort = ftpServerService.getFtpServerPort();
                try {
                    ftpServerEndpoint = NetworkUtilities.getIpEndpointString(deviceIpAddress, ftpServerPort);
                } catch (Exception ignored) {}
            }
        }
        textView_ftpServerEndpoint.setText(ftpServerEndpoint);
        textView_ftpServerEndpoint.requestLayout();

        linearLayout_ftpServerIndicators.setVisibility(ftpServerIsRunningOrBeingStopped ? View.VISIBLE : View.INVISIBLE);
        Drawable ftpServerUserIndicatorDrawable = null;
        Drawable ftpServerEncryptionIndicatorDrawable = null;
        Drawable ftpServerRwIndicatorDrawable = null;
        if (ftpServerIsRunningOrBeingStopped) {
            FtpServerWrapper.FtpUser ftpServerUser = ftpServerService.getFtpServerUser();
            ftpServerUserIndicatorDrawable = AppCompatResources.getDrawable(
                this,
                ((ftpServerUser == null) ?
                    R.drawable.user_disabled_indicator :
                    R.drawable.user_enabled_indicator)
            );
            FtpServerWrapper.FtpEncryption ftpServerEncryption = ftpServerService.getFtpServerEncryption();
            ftpServerEncryptionIndicatorDrawable = AppCompatResources.getDrawable(
                this,
                ((ftpServerEncryption == null) ?
                    R.drawable.encryption_disabled_indicator :
                    (ftpServerEncryption.isForcingEncryption() ?
                        R.drawable.encryption_enabled_indicator :
                        R.drawable.encryption_supported_indicator))
            );
            ftpServerRwIndicatorDrawable = AppCompatResources.getDrawable(
                this,
                (ftpServerService.isFtpServerWritingEnabled() ?
                    R.drawable.read_and_write_indicator :
                    R.drawable.read_only_indicator)
            );
        }
        imageView_ftpServerUserIndicator.setImageDrawable(ftpServerUserIndicatorDrawable);
        imageView_ftpServerEncryptionIndicator.setImageDrawable(ftpServerEncryptionIndicatorDrawable);
        imageView_ftpServerRwIndicator.setImageDrawable(ftpServerRwIndicatorDrawable);
    }
}
