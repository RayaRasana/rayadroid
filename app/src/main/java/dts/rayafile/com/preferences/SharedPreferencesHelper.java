package dts.rayafile.com.preferences;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import dts.rayafile.com.SeadroidApplication;

public class SharedPreferencesHelper {
    public static SharedPreferences getSharedPreferences(String nameSuffix) {
        if (nameSuffix == null) {
            return PreferenceManager.getDefaultSharedPreferences(SeadroidApplication.getInstance());
        } else {
            String pName = PreferenceManagerCompat.getDefaultSharedPreferencesName(SeadroidApplication.getInstance(), nameSuffix);

            int mode = PreferenceManagerCompat.getDefaultSharedPreferencesMode();
            return SeadroidApplication.getInstance().getSharedPreferences(pName, mode);
        }
    }
}
