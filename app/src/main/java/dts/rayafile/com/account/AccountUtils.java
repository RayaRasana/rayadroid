package dts.rayafile.com.account;

import android.webkit.CookieManager;
import android.webkit.ValueCallback;

import com.blankj.utilcode.util.NotificationUtils;
import dts.rayafile.com.framework.datastore.DataStoreManager;
import dts.rayafile.com.framework.datastore.sp_livedata.GestureLockSharePreferenceHelper;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.preferences.ContextStackPreferenceHelper;
import dts.rayafile.com.preferences.Settings;
import dts.rayafile.com.ssl.CertsManager;
import dts.rayafile.com.ui.camera_upload.CameraUploadManager;

public class AccountUtils {

    public static void logout(Account account) {

        // turn off the gesture lock
        GestureLockSharePreferenceHelper.disable();

        Settings.initUserSettings();

        // clear
        ContextStackPreferenceHelper.clearStack();

        //
        CertsManager.instance().deleteCertForAccount(account);

        NotificationUtils.cancelAll();

        // sign out operations
        SupportAccountManager.getInstance().signOutAccount(account);
        SupportAccountManager.getInstance().saveCurrentAccount(null);

        // disable camera upload
        CameraUploadManager.getInstance().disableSpecialAccountCameraUpload(account);

        //cancel all jobs
        BackgroundJobManagerImpl.getInstance().cancelAllJobs();

        //reset IO instance for new account
        HttpIO.resetLoggedInInstance();

        //clear instance
        DataStoreManager.resetUserInstance();

        //clear cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                SLogs.d("removeAllCookie? " + value);
            }
        });
    }

    public static void switchAccount(Account account) {
        if (account == null) {
            return;
        }

        NotificationUtils.cancelAll();

        // clear
        ContextStackPreferenceHelper.clearStack();

        //
        Settings.initUserSettings();

        //switch camera upload
        CameraUploadManager.getInstance().setCameraAccount(account);

        //cancel all jobs
        BackgroundJobManagerImpl.getInstance().cancelAllJobs();

        //reset IO instance for new account
        HttpIO.resetLoggedInInstance();

        //clear instance
        DataStoreManager.resetUserInstance();

        //clear cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                SLogs.d("removeAllCookie? " + value);
            }
        });
    }
}
