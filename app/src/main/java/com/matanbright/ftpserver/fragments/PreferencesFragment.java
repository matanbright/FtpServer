package com.matanbright.ftpserver.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.matanbright.ftpserver.R;
import com.matanbright.ftpserver.activities.AboutActivity;
import com.matanbright.ftpserver.services.FtpServerService;

import java.security.KeyStore;
import java.util.Objects;


public class PreferencesFragment extends PreferenceFragmentCompat {

    private PreferenceCategory preferenceCategory_ftpServerSettings;
    private EditTextPreference editTextPreference_ftpServerPort;
    private CheckBoxPreference checkBoxPreference_ftpServerEnableAuthentication;
    private EditTextPreference editTextPreference_ftpServerUserName;
    private EditTextPreference editTextPreference_ftpServerUserPassword;
    private CheckBoxPreference checkBoxPreference_ftpServerEnableEncryption;
    private EditTextPreference editTextPreference_ftpServerEncryptionKeystoreFilePath;
    private EditTextPreference editTextPreference_ftpServerEncryptionKeyAlias;
    private EditTextPreference editTextPreference_ftpServerEncryptionKeyPassword;
    private CheckBoxPreference checkBoxPreference_ftpServerForceEncryption;
    private CheckBoxPreference checkBoxPreference_ftpServerEnableWriting;
    private ListPreference listPreference_keepDeviceAwake;
    private Preference preference_about;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        preferenceCategory_ftpServerSettings = Objects.requireNonNull(findPreference(getString(R.string.preference_category_key__ftp_server_settings)));
        editTextPreference_ftpServerPort = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_port)));
        editTextPreference_ftpServerPort.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        });
        checkBoxPreference_ftpServerEnableAuthentication = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_enable_authentication)));
        checkBoxPreference_ftpServerEnableAuthentication.setOnPreferenceClickListener(this::onPreferenceClick);
        editTextPreference_ftpServerUserName = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_user_name)));
        editTextPreference_ftpServerUserPassword = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_user_password)));
        checkBoxPreference_ftpServerEnableEncryption = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_enable_encryption)));
        checkBoxPreference_ftpServerEnableEncryption.setOnPreferenceClickListener(this::onPreferenceClick);
        editTextPreference_ftpServerEncryptionKeystoreFilePath = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_encryption_keystore_file_path)));
        editTextPreference_ftpServerEncryptionKeyAlias = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_encryption_key_alias)));
        editTextPreference_ftpServerEncryptionKeyPassword = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_encryption_key_password)));
        checkBoxPreference_ftpServerForceEncryption = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_force_encryption)));
        checkBoxPreference_ftpServerEnableWriting = Objects.requireNonNull(findPreference(getString(R.string.preference_key__ftp_server_enable_writing)));
        listPreference_keepDeviceAwake = Objects.requireNonNull(findPreference(getString(R.string.preference_key__keep_device_awake)));
        preference_about = Objects.requireNonNull(findPreference(getString(R.string.preference_key__about)));
        preference_about.setOnPreferenceClickListener(this::onPreferenceClick);
        updateUi();
    }

    private boolean onPreferenceClick(@NonNull Preference preference) {
        String preferenceKey = preference.getKey();
        if (preferenceKey.equals(getString(R.string.preference_key__ftp_server_enable_authentication))) {
            if (!checkBoxPreference_ftpServerEnableAuthentication.isChecked()) {
                editTextPreference_ftpServerUserName.setText(null);
                editTextPreference_ftpServerUserPassword.setText(null);
            }
        } else if (preferenceKey.equals(getString(R.string.preference_key__ftp_server_enable_encryption))) {
            if (checkBoxPreference_ftpServerEnableEncryption.isChecked()) {
                String defaultKeystoreType = KeyStore.getDefaultType();
                String toastMessage = String.format(getString(R.string.message__supported_keystore_type), defaultKeystoreType);
                Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
            } else {
                editTextPreference_ftpServerEncryptionKeystoreFilePath.setText(null);
                editTextPreference_ftpServerEncryptionKeyAlias.setText(null);
                editTextPreference_ftpServerEncryptionKeyPassword.setText(null);
                checkBoxPreference_ftpServerForceEncryption.setChecked(false);
            }
        } else if (preferenceKey.equals(getString(R.string.preference_key__about))) {
            showAboutActivity();
        }
        updateUi();
        return true;
    }

    private void showAboutActivity() {
        Intent intent = new Intent(getContext(), AboutActivity.class);
        startActivity(intent);
    }

    private void updateUi() {
        boolean ftpServerServiceIsRunning = (FtpServerService.getInstance() != null);
        preferenceCategory_ftpServerSettings.setEnabled(!ftpServerServiceIsRunning);
        editTextPreference_ftpServerUserName.setVisible(checkBoxPreference_ftpServerEnableAuthentication.isChecked());
        editTextPreference_ftpServerUserPassword.setVisible(checkBoxPreference_ftpServerEnableAuthentication.isChecked());
        editTextPreference_ftpServerEncryptionKeystoreFilePath.setVisible(checkBoxPreference_ftpServerEnableEncryption.isChecked());
        editTextPreference_ftpServerEncryptionKeyAlias.setVisible(checkBoxPreference_ftpServerEnableEncryption.isChecked());
        editTextPreference_ftpServerEncryptionKeyPassword.setVisible(checkBoxPreference_ftpServerEnableEncryption.isChecked());
        checkBoxPreference_ftpServerForceEncryption.setVisible(checkBoxPreference_ftpServerEnableEncryption.isChecked());
    }
}
