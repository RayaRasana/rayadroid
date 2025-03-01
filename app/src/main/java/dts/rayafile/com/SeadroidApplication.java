package dts.rayafile.com;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.jeremyliao.liveeventbus.LiveEventBus;
import dts.rayafile.com.enums.NightMode;
import dts.rayafile.com.framework.datastore.sp.Sorts;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.gesturelock.AppLockManager;
import dts.rayafile.com.framework.monitor.ActivityMonitor;
import dts.rayafile.com.framework.notification.base.NotificationUtils;
import dts.rayafile.com.framework.util.CrashHandler;
import dts.rayafile.com.preferences.Settings;


public class SeadroidApplication extends Application {
    private static Context context;
    private static SeadroidApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        //
        Sorts.init();

        //init slogs
        SLogs.init();

        //print current app env info
        SLogs.printAppEnvInfo();

        //
        Settings.initUserSettings();
        if (Settings.APP_NIGHT_MODE != null) {
            NightMode nightMode = Settings.APP_NIGHT_MODE.queryValue();
            AppCompatDelegate.setDefaultNightMode(nightMode.ordinal());
        } else {
            AppCompatDelegate.setDefaultNightMode(NightMode.FOLLOW_SYSTEM.ordinal());
        }


        // set gesture lock if available
        //
//        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        LiveEventBus.config()
                .autoClear(true)
                .enableLogger(BuildConfig.DEBUG)
                .setContext(this)
                .lifecycleObserverAlwaysActive(true);

        //
        NotificationUtils.initNotificationChannels(this);

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);

        //This feature can be extended
        registerActivityLifecycleCallbacks(new ActivityMonitor());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        context = this;
    }

    public static Context getAppContext() {
        return context;
    }

    public static SeadroidApplication getInstance() {
        return instance;
    }

}
