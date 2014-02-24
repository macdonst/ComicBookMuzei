package com.simonmacdonald.muzei.comic;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class ComicCoverSettingsActivity extends PreferenceActivity  {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.settings);
    }

}
