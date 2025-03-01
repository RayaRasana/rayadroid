/*
 * App passcode library for Android, master branch
 * Dual licensed under MIT, and GPL.
 * See https://github.com/wordpress-mobile/Android-PasscodeLock
 */
package dts.rayafile.com.gesturelock;

import android.app.Application;

/**
 * AppLock Manager
 */
public class AppLockManager {

    private static AppLockManager instance;
    private AbstractAppLock currentAppLocker;

    public static AppLockManager getInstance() {
        if (instance == null) {
            instance = new AppLockManager();
        }
        return instance;
    }

    public void enableDefaultAppLockIfAvailable(Application currentApp) {
        currentAppLocker = new DefaultAppLock(currentApp);
        currentAppLocker.enable();
    }
}
