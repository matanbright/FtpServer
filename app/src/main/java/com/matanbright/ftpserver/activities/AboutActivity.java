package com.matanbright.ftpserver.activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.matanbright.ftpserver.R;


public class AboutActivity extends AppCompatActivity {

    private static final String LICENSE_FILES_ASSET_DIRECTORY_PATH = "file:///android_asset/licenses";
    private static final String LICENSE_FILE_ASSET_PATH__APACHE_LICENSE = LICENSE_FILES_ASSET_DIRECTORY_PATH + "/LICENSE.txt";
    private static final String LICENSE_FILE_ASSET_PATH__MIT_LICENSE = LICENSE_FILES_ASSET_DIRECTORY_PATH + "/LICENSE-MIT.txt";

    private TextView textView_viewApacheLicense;
    private TextView textView_viewMitLicense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        textView_viewApacheLicense = findViewById(R.id.textView_viewApacheLicense);
        textView_viewApacheLicense.setOnClickListener(this::onTextViewClick);
        textView_viewMitLicense = findViewById(R.id.textView_viewMitLicense);
        textView_viewMitLicense.setOnClickListener(this::onTextViewClick);
    }

    private void onTextViewClick(@NonNull View view) {
        int viewId = view.getId();
        if (viewId == R.id.textView_viewApacheLicense)
            viewLicenseFile(LICENSE_FILE_ASSET_PATH__APACHE_LICENSE);
        else if (viewId == R.id.textView_viewMitLicense)
            viewLicenseFile(LICENSE_FILE_ASSET_PATH__MIT_LICENSE);
    }

    private void viewLicenseFile(String licenseFileAssetPath) {
        try {
            WebView webView = new WebView(this);
            webView.loadUrl(licenseFileAssetPath);
            AlertDialog alertDialog = (new AlertDialog.Builder(this))
                .setView(webView)
                .setPositiveButton(getString(R.string.button_text__close), ((dialog, which) -> dialog.dismiss()))
                .create();
            alertDialog.show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_message__unable_to_open_the_file), Toast.LENGTH_SHORT).show();
        }
    }
}
