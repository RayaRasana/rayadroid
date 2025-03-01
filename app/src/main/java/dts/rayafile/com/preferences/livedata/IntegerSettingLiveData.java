package dts.rayafile.com.preferences.livedata;

import android.content.SharedPreferences;

import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;

import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.preferences.SettingsLiveData;

public class IntegerSettingLiveData extends SettingsLiveData<Integer> {

    public IntegerSettingLiveData(@StringRes int keyRes, @IntegerRes int defaultValueRes) {
        this(null, keyRes, defaultValueRes);
    }

    public IntegerSettingLiveData(String nameSuffix, @StringRes int keyRes, @IntegerRes int defaultValueRes) {
        super(nameSuffix, keyRes, defaultValueRes);

        register();
    }

    @Override
    protected Integer getDefaultValue(@IntegerRes int defaultValueRes) {
        return SeadroidApplication.getInstance().getResources().getInteger(defaultValueRes);
    }

    @Override
    protected Integer getValue(SharedPreferences sharedPreferences, String key, Integer defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    @Override
    protected void putValue(SharedPreferences sharedPreferences, String key, Integer value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }
}
