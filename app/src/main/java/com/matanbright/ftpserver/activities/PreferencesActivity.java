package com.matanbright.ftpserver.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.matanbright.ftpserver.fragments.PreferencesFragment;


public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new PreferencesFragment())
                .commit();
        }
    }
}
