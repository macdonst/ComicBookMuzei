package com.simonmacdonald.muzei.comic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Utils {
	public static final boolean CONNECTION_WIFI = true;

    public static final int MIN_FREQ_MILLIS = 3 * 60 * 60 * 1000;

    private static final int DEFAULT_FREQ_MILLIS = 24 * 60 * 60 * 1000;

    public static boolean getConfigConnection(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getBoolean("network_preference", CONNECTION_WIFI);
    }

    public static void setConfigConnection(Context context, boolean connection) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putBoolean("network_preference", connection).commit();
    }

    public static void setConfigFreq(Context context, String numHours) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putString("refresh_preference", numHours).commit();
    }

    public static String getConfigFreq(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getString("refresh_preference", "24");
    }
    
    private static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
