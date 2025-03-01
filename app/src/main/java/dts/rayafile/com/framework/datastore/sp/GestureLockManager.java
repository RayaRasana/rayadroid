package dts.rayafile.com.framework.datastore.sp;

import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.framework.datastore.DataStoreManager;
import dts.rayafile.com.gesturelock.LockPatternUtils;

@Deprecated
public class GestureLockManager {

    public static long lock_timestamp = 0;
    public static final long LOCK_EXPIRATION_MSECS = 5 * 60 * 1000;

    public static boolean readGestureLockSwitch() {
        return DataStoreManager.getCommonSharePreference().readBoolean(SettingsManager.GESTURE_LOCK_SWITCH_KEY, false);
    }

    public static void writeGestureLockSwitch(boolean isChecked) {
        DataStoreManager.getCommonSharePreference().writeBoolean(SettingsManager.GESTURE_LOCK_SWITCH_KEY, isChecked);
        saveGestureLockTimeStamp();
    }

    /**
     * For convenience, if the user has given the correct gesture lock, he
     * would not be asked for gesture lock for a short period of time.
     */
    public static boolean isGestureLockRequired() {
        boolean isEnable = DataStoreManager.getCommonSharePreference().readBoolean(SettingsManager.GESTURE_LOCK_SWITCH_KEY);
        if (!isEnable) {
            return false;
        }

        LockPatternUtils mLockPatternUtils = new LockPatternUtils(SeadroidApplication.getAppContext());
        if (!mLockPatternUtils.savedPatternExists()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now < lock_timestamp + LOCK_EXPIRATION_MSECS) {
            return false;
        }

        return true;
    }

    public static void saveGestureLockTimeStamp() {
        lock_timestamp = System.currentTimeMillis();
    }

}
