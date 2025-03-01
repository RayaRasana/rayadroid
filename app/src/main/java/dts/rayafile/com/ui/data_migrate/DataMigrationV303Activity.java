package dts.rayafile.com.ui.data_migrate;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.CollectionUtils;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.databinding.ActivityDataMigrationBinding;
import dts.rayafile.com.framework.datastore.DataStoreKeys;
import dts.rayafile.com.framework.datastore.DataStoreManager;
import dts.rayafile.com.framework.datastore.sp.AlbumBackupManager;
import dts.rayafile.com.framework.datastore.sp.AppDataManager;
import dts.rayafile.com.framework.datastore.sp.FolderBackupManager;
import dts.rayafile.com.framework.datastore.sp.SettingsManager;
import dts.rayafile.com.framework.datastore.sp_livedata.AlbumBackupSharePreferenceHelper;
import dts.rayafile.com.framework.datastore.sp_livedata.FolderBackupSharePreferenceHelper;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.preferences.Settings;
import dts.rayafile.com.enums.NetworkMode;
import dts.rayafile.com.ui.folder_backup.RepoConfig;

import java.util.List;

/**
 * Migrating data from sqlite database to Room
 */
public class DataMigrationV303Activity extends AppCompatActivity {
    private ActivityDataMigrationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDataMigrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List<Account> accounts = SupportAccountManager.getInstance().getAccountList();
        if (CollectionUtils.isEmpty(accounts)) {
            finishMigration();
            return;
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                //do nothing
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    startMigration(accounts);

                } catch (Exception e) {
                    SLogs.e(e);
                } finally {
                    finishMigration();
                }
            }
        }).start();
    }

    private void startMigration(List<Account> accounts) {
        long now = System.currentTimeMillis();

        for (Account account : accounts) {
            //
            Settings.initUserSettings(account);

            String repoKV = DataStoreManager.getInstanceByUser(account.getSignature()).readString(DataStoreKeys.DS_REPO_DIR_MAPPING);
            if (!TextUtils.isEmpty(repoKV)) {
                Settings.getCurrentAccountSharedPreferences().edit().putString(DataStoreKeys.DS_REPO_DIR_MAPPING, repoKV).commit();
            }


            //gesture lock
            boolean isGestureLockEnable = DataStoreManager.getCommonSharePreference().readBoolean(SettingsManager.GESTURE_LOCK_SWITCH_KEY);
//            Settings.USER_GESTURE_LOCK_SWITCH.putValue(isGestureLockEnable);
            Settings.SETTINGS_GESTURE.putValue(isGestureLockEnable);
            if (isGestureLockEnable) {
//                Settings.USER_GESTURE_LOCK_TIMESTAMP.putValue(now);
                Settings.SETTINGS_GESTURE_LOCK_TIMESTAMP.putValue(now);
            }

            migrateAlbumSP();

            migrateFolderBackupSP();
        }
    }

    private void migrateAlbumSP() {
        boolean backupSwitch = AlbumBackupManager.readBackupSwitch();
        AlbumBackupSharePreferenceHelper.writeBackupSwitch(backupSwitch);

        RepoConfig repoConfig = AlbumBackupManager.readRepoConfig();
        AlbumBackupSharePreferenceHelper.writeRepoConfig(repoConfig);

        long lastScanTime = AlbumBackupManager.readLastScanTime();
        AlbumBackupSharePreferenceHelper.writeLastScanTime(lastScanTime);

        boolean customSwitch = AlbumBackupManager.readCustomAlbumSwitch();
        AlbumBackupSharePreferenceHelper.writeCustomAlbumSwitch(customSwitch);

        boolean dataPlanSwitch = AlbumBackupManager.readAllowDataPlanSwitch();
        AlbumBackupSharePreferenceHelper.writeAllowDataPlanSwitch(dataPlanSwitch);

        boolean videoSwitch = AlbumBackupManager.readAllowVideoSwitch();
        AlbumBackupSharePreferenceHelper.writeAllowVideoSwitch(videoSwitch);

        List<String> ids = AlbumBackupManager.readBucketIds();
        AlbumBackupSharePreferenceHelper.writeBucketIds(ids);

        SLogs.eDebug("----------album backup sp-----------");
        SLogs.eDebug("backupSwitch = " + backupSwitch);
        SLogs.eDebug("repoConfig = " + (repoConfig == null ? "null" : repoConfig.toString()));
        SLogs.eDebug("lastScanTime = " + lastScanTime);
        SLogs.eDebug("customSwitch = " + customSwitch);
        SLogs.eDebug("dataPlanSwitch = " + dataPlanSwitch);
        SLogs.eDebug("videoSwitch = " + videoSwitch);
        if (!CollectionUtils.isEmpty(ids)) {
            for (int i = 0; i < ids.size(); i++) {
                SLogs.eDebug(i + ", " + "ids = " + ids.get(i));
            }
        } else {
            SLogs.eDebug("ids is empty");
        }
    }

    private void migrateFolderBackupSP() {
        FolderBackupSharePreferenceHelper.writeSkipHiddenFiles(true);

        boolean backupSwitch = FolderBackupManager.readBackupSwitch();
        FolderBackupSharePreferenceHelper.writeBackupSwitch(backupSwitch);

        List<String> paths = FolderBackupManager.readBackupPaths();
        FolderBackupSharePreferenceHelper.writeBackupPathsAsString(paths);

        RepoConfig repoConfig = FolderBackupManager.readRepoConfig();
        FolderBackupSharePreferenceHelper.writeRepoConfig(repoConfig);

        long lastScanTime = FolderBackupManager.readLastScanTime();
        FolderBackupSharePreferenceHelper.writeLastScanTime(lastScanTime);

        String dataPlan = FolderBackupManager.readNetworkMode();
        if ("WIFI".equals(dataPlan)) {
            FolderBackupSharePreferenceHelper.writeNetworkMode(NetworkMode.WIFI);
        } else {
            FolderBackupSharePreferenceHelper.writeNetworkMode(NetworkMode.WIFI_AND_MOBILE);
        }


        SLogs.eDebug("----------folder backup sp-----------");
        SLogs.eDebug("backupSwitch = " + backupSwitch);
        SLogs.eDebug("repoConfig = " + (repoConfig == null ? "null" : repoConfig.toString()));
        SLogs.eDebug("lastScanTime = " + lastScanTime);
        SLogs.eDebug("dataPlan = " + dataPlan);
        if (!CollectionUtils.isEmpty(paths)) {
            for (int i = 0; i < paths.size(); i++) {
                SLogs.eDebug(i + ", " + "paths = " + paths.get(i));
            }
        } else {
            SLogs.eDebug("paths is empty");
        }

    }

    private void finishMigration() {
        AppDataManager.setMigratedWhenV303(1);
        SLogs.eDebug("finishMigrationV303");

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true; //do not back
        }
        return super.onKeyDown(keyCode, event);
    }
}
