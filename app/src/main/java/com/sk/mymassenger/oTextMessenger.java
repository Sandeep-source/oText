package com.sk.mymassenger;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.Preference;
import android.preference.PreferenceManager;

public class oTextMessenger extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preference=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //int theme=preference.getInt("theme",-1);
        //if(theme!=-1)
        setTheme(R.style.oText_Theme_Cyan);
    }
}
