/*
 * App passcode library for Android, master branch
 * Dual licensed under MIT, and GPL.
 * See https://github.com/wordpress-mobile/Android-PasscodeLock
 */
package dts.rayafile.com.gesturelock;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.common.collect.MapMaker;
import dts.rayafile.com.framework.datastore.sp_livedata.GestureLockSharePreferenceHelper;
import dts.rayafile.com.ui.SplashActivity;
import dts.rayafile.com.ui.gesture.UnlockGesturePasswordActivity;

import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of AppLock
 */
public class DefaultAppLock extends AbstractAppLock {
    public static final String DEBUG_TAG = "DefaultAppLock";

    private final Application currentApp; //Keep a reference to the app that invoked the locker
    /**
     * by default, the returned map uses equality comparisons (the equals method) to determine equality for keys or values.
     * However, if weakKeys() was specified, the map uses identity (==) comparisons instead for keys.
     * Likewise, if weakValues() or softValues() was specified, the map uses identity (==) comparisons for values.
     */
    private static final ConcurrentMap<Object, Long> mCheckedActivities = new MapMaker()
            .weakKeys()
            .makeMap();

    public DefaultAppLock(Application currentApp) {
        super();
        this.currentApp = currentApp;
    }

    public void enable() {
        currentApp.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void disable() {
        currentApp.unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(DEBUG_TAG, "onActivityPaused");

        if (activity.getClass() == UnlockGesturePasswordActivity.class) {
            return;
        }

        if (activity.getClass() == SplashActivity.class) {
            return;
        }

        if (!isActivityBeingChecked(activity)) {
            GestureLockSharePreferenceHelper.updateLockTimeStamp();
        }
    }

    private boolean isActivityBeingChecked(Activity activity) {
        if (!mCheckedActivities.containsKey(activity)) {
            return false;
        }

        long ts = mCheckedActivities.get(activity);
        return ts + 2000 > System.currentTimeMillis();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d(DEBUG_TAG, "onActivityResumed");

        /** just compare fully-qualified names to determine if two classes being equal
         * even if they've been loaded by different classloaders,
         * possibly from different locations */
        if (activity.getClass() == UnlockGesturePasswordActivity.class) {
            return;
        }

        if (activity.getClass() == SplashActivity.class) {
            return;
        }

        if (GestureLockSharePreferenceHelper.isLockRequired()) {
            mCheckedActivities.put(activity, System.currentTimeMillis());
            Intent i = new Intent(activity, UnlockGesturePasswordActivity.class);
            activity.startActivity(i);
        }

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
