package dts.rayafile.com.preferences.livedata;

import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.AnyRes;
import androidx.annotation.StringRes;

import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.annotation.Unstable;
import dts.rayafile.com.preferences.SettingsLiveData;

@Unstable
public class ResourceIdSettingLiveData extends SettingsLiveData<Integer> {
    public ResourceIdSettingLiveData(@StringRes int keyRes, @AnyRes int defaultValueRes) {
        this(null, keyRes, defaultValueRes);
    }

    public ResourceIdSettingLiveData(String nameSuffix, @StringRes int keyRes, @AnyRes int defaultValueRes) {
        this(nameSuffix, keyRes, null, defaultValueRes);
    }

    public ResourceIdSettingLiveData(String nameSuffix, @StringRes int keyRes, String keySuffix, @AnyRes int defaultValueRes) {
        super(nameSuffix, keyRes, keySuffix, defaultValueRes);

        register();
    }

    @Override
    protected Integer getDefaultValue(@AnyRes int defaultValueRes) {
        return defaultValueRes;
    }

    @Override
    protected Integer getValue(SharedPreferences sharedPreferences, String key, Integer defaultValue) {
        String string = sharedPreferences.getString(key, null);
        if (TextUtils.isEmpty(string)) {
            return defaultValue;
        }

        int v = SeadroidApplication.getInstance().getResources().getIdentifier(string, null, SeadroidApplication.getInstance().getPackageName());
        if (v != 0) {
            return v;
        }

        return defaultValue;
    }

    @Override
    protected void putValue(SharedPreferences sharedPreferences, String key, @AnyRes Integer value) {
        String n = SeadroidApplication.getInstance().getResources().getResourceName(value);
        sharedPreferences.edit().putString(key, n).apply();
    }
}
