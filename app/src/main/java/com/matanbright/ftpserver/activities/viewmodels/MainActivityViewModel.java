package com.matanbright.ftpserver.activities.viewmodels;

import androidx.lifecycle.ViewModel;


public class MainActivityViewModel extends ViewModel {

    private boolean permissionsHaveBeenRequested;

    public MainActivityViewModel() {
        super();
        permissionsHaveBeenRequested = false;
    }

    public boolean havePermissionsBeenRequested() {
        return permissionsHaveBeenRequested;
    }

    public void markThatPermissionsHaveBeenRequested() {
        permissionsHaveBeenRequested = true;
    }
}
