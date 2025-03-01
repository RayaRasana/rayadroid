package dts.rayafile.com.framework.helper;

import androidx.appcompat.app.AppCompatDelegate;

import dts.rayafile.com.enums.NightMode;
import dts.rayafile.com.preferences.Settings;

import java.util.Objects;

public class NightModeHelper {

    public static void sync() {
        AppCompatDelegate.setDefaultNightMode(getNightMode());
    }

    public static void syncTo(NightMode nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode.ordinal());
        Settings.APP_NIGHT_MODE.putValue(nightMode);
    }


    public static int getNightMode() {
        NightMode nightMode1 = Settings.APP_NIGHT_MODE.getValue();
        return Objects.requireNonNullElse(nightMode1, NightMode.FOLLOW_SYSTEM).ordinal();
    }

}
