package dts.rayafile.com.preferences.livedata;

import android.content.SharedPreferences;

import androidx.annotation.BoolRes;
import androidx.annotation.StringRes;

import dts.rayafile.com.R;
import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.preferences.SettingsLiveData;

public class BooleanSettingLiveData extends SettingsLiveData<Boolean> {

    public BooleanSettingLiveData(@StringRes int keyRes) {
        this(null, keyRes, R.bool.pref_default_false);
    }

    public BooleanSettingLiveData(String nameSuffix, @StringRes int keyRes) {
        this(nameSuffix, keyRes, R.bool.pref_default_false);
    }

    public BooleanSettingLiveData(@StringRes int keyRes, @BoolRes int defaultValueRes) {
        this(null, keyRes, defaultValueRes);
    }

    public BooleanSettingLiveData(String nameSuffix, @StringRes int keyRes, @BoolRes int defaultValueRes) {
        super(nameSuffix, keyRes, defaultValueRes);

        register();
    }

    @Override
    protected Boolean getDefaultValue(@BoolRes int defaultValueRes) {
        return SeadroidApplication.getInstance().getResources().getBoolean(defaultValueRes);
    }

    @Override
    protected Boolean getValue(SharedPreferences sharedPreferences, String key, Boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    @Override
    protected void putValue(SharedPreferences sharedPreferences, String key, Boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }
}
