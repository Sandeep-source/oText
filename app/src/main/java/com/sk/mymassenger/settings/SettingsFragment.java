package com.sk.mymassenger.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;

import com.sk.mymassenger.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource( R.xml.root_preferences, rootKey );
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener( this );

    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Toast.makeText(requireContext(),"tiggeresd change",Toast.LENGTH_SHORT).show();
        if(key.equals( "DarkTheme" )){
            boolean enabled=sharedPreferences.getBoolean( key,false );
            if(enabled){
                AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_YES );
                Toast.makeText(requireContext(),"to night",Toast.LENGTH_SHORT).show();
            }else{
                AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_NO );
                Toast.makeText(requireContext(),"to day",Toast.LENGTH_SHORT).show();
            }

        }
    }
}