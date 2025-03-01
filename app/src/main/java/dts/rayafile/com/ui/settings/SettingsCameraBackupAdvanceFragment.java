package dts.rayafile.com.ui.settings;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import dts.rayafile.com.R;
import dts.rayafile.com.framework.datastore.sp.AlbumBackupManager;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.ui.camera_upload.CameraUploadConfigActivity;
import dts.rayafile.com.ui.camera_upload.GalleryBucketUtils;
import dts.rayafile.com.framework.datastore.sp.SettingsManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Deprecated
public class SettingsCameraBackupAdvanceFragment extends PreferenceFragmentCompat {

    private SwitchPreferenceCompat mCameraBackupCustomBucketsSwitch;
    private SwitchPreferenceCompat cbDataPlan;
    private SwitchPreferenceCompat cbVideoAllowed;
    private Preference mCameraBackupLocalBucketPref;

    public static SettingsCameraBackupAdvanceFragment newInstance() {

        Bundle args = new Bundle();

        SettingsCameraBackupAdvanceFragment fragment = new SettingsCameraBackupAdvanceFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.prefs_settings_camera_backup_advance, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initCameraBackupView();
    }

    private void initCameraBackupView() {

        //data plan
        cbDataPlan = findPreference(SettingsManager.CAMERA_UPLOAD_ALLOW_DATA_PLAN_SWITCH_KEY);

        boolean isDataPlanChecked = AlbumBackupManager.readAllowDataPlanSwitch();
        cbDataPlan.setChecked(isDataPlanChecked);
        cbDataPlan.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                boolean isCustom = (Boolean) newValue;
                AlbumBackupManager.writeAllowDataPlanSwitch(isCustom);

                BackgroundJobManagerImpl.getInstance().startMediaBackupWorkerChain(isCustom);

                return true;
            }
        });

        // videos
        cbVideoAllowed = findPreference(SettingsManager.CAMERA_UPLOAD_ALLOW_VIDEOS_SWITCH_KEY);

        boolean isAllowVideoChecked = AlbumBackupManager.readAllowVideoSwitch();
        cbVideoAllowed.setChecked(isAllowVideoChecked);
        cbVideoAllowed.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                boolean isCustom = (Boolean) newValue;
                AlbumBackupManager.writeAllowVideoSwitch(isCustom);

                BackgroundJobManagerImpl.getInstance().startMediaBackupWorkerChain(isCustom);

                return true;
            }
        });

        //custom album
        mCameraBackupCustomBucketsSwitch = findPreference(SettingsManager.CAMERA_UPLOAD_CUSTOM_BUCKETS_KEY);

        boolean isCustomChecked = AlbumBackupManager.readCustomAlbumSwitch();
        mCameraBackupCustomBucketsSwitch.setChecked(isCustomChecked);

        mCameraBackupCustomBucketsSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isCustom = (Boolean) newValue;
            AlbumBackupManager.writeCustomAlbumSwitch(isCustom);

            mCameraBackupLocalBucketPref.setVisible(isCustom);
            scanCustomDirs(isCustom);
            return false;
        });

        //
        mCameraBackupLocalBucketPref = findPreference(SettingsManager.CAMERA_UPLOAD_BUCKETS_KEY);

        mCameraBackupLocalBucketPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                // choose media buckets
                scanCustomDirs(true);

                return true;
            }
        });

        mCameraBackupLocalBucketPref.setVisible(mCameraBackupCustomBucketsSwitch.isChecked());

        refreshPreferenceView();
    }

    private void scanCustomDirs(boolean isCustomScanOn) {
        if (isCustomScanOn) {
            Intent intent = new Intent(requireActivity(), CameraUploadConfigActivity.class);
            intent.putExtra(CameraUploadConfigActivity.CAMERA_UPLOAD_LOCAL_DIRECTORIES, true);
            selectLocalDirLauncher.launch(intent);
        } else {
            List<String> selectedBuckets = new ArrayList<>();
            AlbumBackupManager.writeBucketIds(selectedBuckets);

            BackgroundJobManagerImpl.getInstance().startMediaBackupWorkerChain(false);

            refreshPreferenceView();
        }
    }

    private void refreshPreferenceView() {
        List<String> bucketNames = new ArrayList<>();


        List<String> bucketIds = AlbumBackupManager.readBucketIds();
        List<GalleryBucketUtils.Bucket> tempBuckets = GalleryBucketUtils.getMediaBuckets(getActivity().getApplicationContext());
        LinkedHashSet<GalleryBucketUtils.Bucket> bucketsSet = new LinkedHashSet<>(tempBuckets.size());
        bucketsSet.addAll(tempBuckets);
        List<GalleryBucketUtils.Bucket> allBuckets = new ArrayList<>(bucketsSet.size());
        allBuckets.addAll(bucketsSet);

        for (GalleryBucketUtils.Bucket bucket : allBuckets) {
            if (bucketIds.contains(bucket.bucketId)) {
                bucketNames.add(bucket.bucketName);
            }
        }

        if (bucketNames.isEmpty()) {
            AlbumBackupManager.writeCustomAlbumSwitch(false);
            mCameraBackupCustomBucketsSwitch.setChecked(false);
            mCameraBackupLocalBucketPref.setVisible(false);
        } else {
            AlbumBackupManager.writeCustomAlbumSwitch(true);
            mCameraBackupCustomBucketsSwitch.setChecked(true);
            mCameraBackupLocalBucketPref.setVisible(true);
            mCameraBackupLocalBucketPref.setSummary(TextUtils.join(", ", bucketNames));
        }
    }

    private final ActivityResultLauncher<Intent> selectLocalDirLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (o.getResultCode() != RESULT_OK) {

                AlbumBackupManager.writeCustomAlbumSwitch(false);
                mCameraBackupCustomBucketsSwitch.setChecked(false);
                mCameraBackupLocalBucketPref.setVisible(false);

                return;
            }

            BackgroundJobManagerImpl.getInstance().startMediaBackupWorkerChain(true);

            refreshPreferenceView();
        }
    });
}
