package dts.rayafile.com.preferences.livedata;

import android.content.SharedPreferences;

import androidx.annotation.StringRes;

import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.preferences.SettingsLiveData;

public class LongSettingLiveData extends SettingsLiveData<Long> {
    public LongSettingLiveData(@StringRes int keyRes, @StringRes int defaultValueRes) {
        this(null, keyRes, defaultValueRes);
    }

    public LongSettingLiveData(String nameSuffix, @StringRes int keyRes, @StringRes int defaultValueRes) {
        this(nameSuffix, keyRes, null, defaultValueRes);
    }

    public LongSettingLiveData(String nameSuffix, @StringRes int keyRes, String keySuffix, @StringRes int defaultValueRes) {
        super(nameSuffix, keyRes, keySuffix, defaultValueRes);

        register();
    }

    @Override
    protected Long getDefaultValue(@StringRes int defaultValueRes) {
        String l = SeadroidApplication.getInstance().getString(defaultValueRes);
        return Long.parseLong(l);
    }

    @Override
    protected Long getValue(SharedPreferences sharedPreferences, String key, Long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    @Override
    protected void putValue(SharedPreferences sharedPreferences, String key, Long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }
}
