package dts.rayafile.com.framework.datastore.sp_livedata;

import dts.rayafile.com.preferences.Settings;

public class ClientEncryptSharePreferenceHelper {

    @Deprecated
    public static void writeClientEncSwitch(boolean isChecked) {
        if (Settings.CLIENT_ENCRYPT_SWITCH == null) {
            return;
        }

        Settings.CLIENT_ENCRYPT_SWITCH.putValue(isChecked);
    }

    public static boolean isEncryptEnabled() {
        return false;

//        if (Settings.CLIENT_ENCRYPT_SWITCH == null) {
//            return false;
//        }
//        return Settings.CLIENT_ENCRYPT_SWITCH.queryValue();
    }
}
