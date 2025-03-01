package dts.rayafile.com.framework.worker;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.framework.datastore.sp_livedata.AlbumBackupSharePreferenceHelper;
import dts.rayafile.com.framework.datastore.sp_livedata.FolderBackupSharePreferenceHelper;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.worker.download.DownloadFileScanWorker;
import dts.rayafile.com.framework.worker.download.DownloadWorker;
import dts.rayafile.com.framework.worker.download.DownloadedFileMonitorWorker;
import dts.rayafile.com.framework.worker.upload.FolderBackupScannerWorker;
import dts.rayafile.com.framework.worker.upload.MediaBackupScannerWorker;
import dts.rayafile.com.framework.worker.upload.UploadFileManuallyWorker;
import dts.rayafile.com.framework.worker.upload.UploadFolderFileAutomaticallyWorker;
import dts.rayafile.com.framework.worker.upload.UploadMediaFileAutomaticallyWorker;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BackgroundJobManagerImpl {
    public static final String TAG_ALL = "*";
    public static final String TAG_TRANSFER = TAG_ALL + ":transfer";

    private final long MAX_CONTENT_TRIGGER_DELAY_MS = 1500L;
    private final long PERIODIC_BACKUP_INTERVAL_MINUTES = 24 * 60L;
    private final long DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES = 15L;

    private BackgroundJobManagerImpl() {

    }

    public static BackgroundJobManagerImpl getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final BackgroundJobManagerImpl INSTANCE = new BackgroundJobManagerImpl();
    }

    private <T extends ListenableWorker> OneTimeWorkRequest.Builder oneTimeRequestBuilder(Class<T> tClass) {
        return new OneTimeWorkRequest.Builder(tClass)
                .addTag(TAG_ALL)
                .addTag(TAG_TRANSFER)
                .addTag(tClass.getSimpleName());
    }

    private <T extends ListenableWorker> PeriodicWorkRequest.Builder periodicRequestBuilder(Class<T> tClass, long intervalMins, long flexIntervalMins) {
        if (intervalMins == 0) {
            intervalMins = DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES;
        }
        if (flexIntervalMins == 0) {
            flexIntervalMins = DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES;
        }
        return new PeriodicWorkRequest.Builder(tClass, intervalMins, TimeUnit.MINUTES, flexIntervalMins, TimeUnit.MINUTES)
                .addTag(TAG_ALL)
                .addTag(TAG_TRANSFER)
                .addTag(tClass.getSimpleName());
    }

    private boolean checkWorkerIsRunningById(UUID uid) {
        ListenableFuture<WorkInfo> listenableFuture = getWorkManager().getWorkInfoById(uid);
        try {
            WorkInfo task = listenableFuture.get();
            if (task == null) {
                return false;
            }

            return !task.getState().isFinished();
        } catch (ExecutionException | InterruptedException e) {
            SLogs.e("checkWorkerIsRunningById", e);
            return false;
        }

    }

    public void cancelById(UUID uid) {
        getWorkManager().cancelWorkById(uid);
    }

    public WorkInfo getWorkInfoById(UUID uid) {
        ListenableFuture<WorkInfo> listener = getWorkManager().getWorkInfoById(uid);
        try {
            return listener.get();
        } catch (ExecutionException | InterruptedException e) {
            SLogs.w("getWorkInfoById", e);
            return null;
        }
    }

    public WorkManager getWorkManager() {
        return WorkManager.getInstance(SeadroidApplication.getAppContext());
    }

    ///////////////////

    /// media worker
    /// ////////////////
    public void startMediaBackupWorkerChain(boolean isForce) {
        cancelMediaBackupWorker();

        OneTimeWorkRequest scanRequest = getMediaScannerWorkerRequest(isForce);
        OneTimeWorkRequest uploadRequest = getMediaUploadWorkerRequest();

        String workerName = MediaBackupScannerWorker.class.getSimpleName();

        getWorkManager()
                .beginUniqueWork(workerName, ExistingWorkPolicy.KEEP, scanRequest)
                .then(uploadRequest)
                .enqueue();
    }

    private OneTimeWorkRequest getMediaScannerWorkerRequest(boolean isForce) {
        Data data = new Data.Builder()
                .putBoolean(TransferWorker.DATA_FORCE_TRANSFER_KEY, isForce)
                .build();

        return oneTimeRequestBuilder(MediaBackupScannerWorker.class)
                .setInputData(data)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .setId(MediaBackupScannerWorker.UID)
                .build();
    }

    private OneTimeWorkRequest getMediaUploadWorkerRequest() {
        NetworkType networkType = NetworkType.UNMETERED;
        boolean isAllowData = AlbumBackupSharePreferenceHelper.readAllowDataPlanSwitch();
        if (isAllowData) {
            networkType = NetworkType.CONNECTED;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build();

        return oneTimeRequestBuilder(UploadMediaFileAutomaticallyWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .setId(UploadMediaFileAutomaticallyWorker.UID)
                .build();
    }

    public void restartMediaBackupWorker() {
        cancelMediaBackupWorker();
        startMediaBackupWorkerChain(false);
    }

    //cancel media
    public void cancelMediaBackupWorker() {
        cancelById(UploadMediaFileAutomaticallyWorker.UID);
        cancelById(MediaBackupScannerWorker.UID);
    }

    ///////////////////

    /// upload folder
    /// ////////////////
    public void startFolderBackupWorkerChain(boolean isForce) {
        cancelFolderBackupWorker();

        OneTimeWorkRequest scanRequest = getFolderBackupScanWorkerRequest(isForce);
        OneTimeWorkRequest uploadRequest = getFolderBackupUploadWorkerRequest();

        String workerName = FolderBackupScannerWorker.class.getSimpleName();

        getWorkManager()
                .beginUniqueWork(workerName, ExistingWorkPolicy.KEEP, scanRequest)
                .then(uploadRequest)
                .enqueue();
    }

    private OneTimeWorkRequest getFolderBackupScanWorkerRequest(boolean isForce) {
        Data data = new Data.Builder()
                .putBoolean(TransferWorker.DATA_FORCE_TRANSFER_KEY, isForce)
                .build();

        return oneTimeRequestBuilder(FolderBackupScannerWorker.class)
                .setInputData(data)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .setId(FolderBackupScannerWorker.UID)
                .build();
    }

    private OneTimeWorkRequest getFolderBackupUploadWorkerRequest() {
        NetworkType networkType = NetworkType.UNMETERED;
        if (FolderBackupSharePreferenceHelper.readDataPlanAllowed()) {
            networkType = NetworkType.CONNECTED;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build();

        return oneTimeRequestBuilder(UploadFolderFileAutomaticallyWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .setId(UploadFolderFileAutomaticallyWorker.UID)
                .build();
    }

    public void restartFolderBackupWorker() {
        cancelFolderBackupWorker();
        startFolderBackupWorkerChain(false);
    }

    public void cancelFolderBackupWorker() {
        cancelById(FolderBackupScannerWorker.UID);
        cancelById(UploadFolderFileAutomaticallyWorker.UID);
    }

    ///////////////////

    /// upload file
    /// ////////////////
    public void startFileManualUploadWorker() {
        String workerName = UploadFileManuallyWorker.class.getSimpleName();
        OneTimeWorkRequest request = getFileUploadRequest();
        getWorkManager().enqueueUniqueWork(workerName, ExistingWorkPolicy.KEEP, request);
    }

    private OneTimeWorkRequest getFileUploadRequest() {
        return oneTimeRequestBuilder(UploadFileManuallyWorker.class)
                .setId(UploadFileManuallyWorker.UID)
                .build();
    }

    public void cancelFileManualUploadWorker() {
        cancelById(UploadFileManuallyWorker.UID);
    }

    ///////////////////
    /// download
    /// ////////////////
    public OneTimeWorkRequest getDownloadScanRequest(String transferId, String[] direntIds) {
        Data.Builder builder = new Data.Builder();
        if (!TextUtils.isEmpty(transferId)) {
            builder.putString(DownloadFileScanWorker.KEY_TRANSFER_ID, transferId);
        }
        if (direntIds != null && direntIds.length > 0) {
            builder.putStringArray(TransferWorker.DATA_DIRENT_LIST_KEY, direntIds);
        }

        return oneTimeRequestBuilder(DownloadFileScanWorker.class)
                .setInputData(builder.build())
                .build();
    }

    private OneTimeWorkRequest getDownloadRequest() {
        return oneTimeRequestBuilder(DownloadWorker.class)
                .setId(DownloadWorker.UID)
                .build();
    }

    public void startDownloadChainWorker() {
        startDownloadChainWorker(null, null,null);
    }

    /**
     * in batches
     */
    public void startDownloadChainWorker(String[] direntIds,final DownloadCallback callback) {
        startDownloadChainWorker(null, direntIds,callback);
    }
    public void startDownloadChainWorker(String[] direntIds) {
        startDownloadChainWorker(null, direntIds,null);
    }

    public void startDownloadChainWorker(String transferId) {
        startDownloadChainWorker(transferId, null,null);
    }

    //    public void startDownloadChainWorker(String transferId, String[] direntIds) {
//
//        OneTimeWorkRequest scanRequest = getDownloadScanRequest(transferId, direntIds);
//        OneTimeWorkRequest downloadRequest = getDownloadRequest();
//
//        String workerName = DownloadFileScanWorker.class.getSimpleName();
//        getWorkManager().beginUniqueWork(workerName, ExistingWorkPolicy.REPLACE, scanRequest)
//                .then(downloadRequest)
//                .enqueue();
//    }
    public void startDownloadChainWorker(String transferId, String[] direntIds, final DownloadCallback callback) {

        OneTimeWorkRequest scanRequest = getDownloadScanRequest(transferId, direntIds);
        OneTimeWorkRequest downloadRequest = getDownloadRequest();

        String workerName = DownloadFileScanWorker.class.getSimpleName();
        WorkManager workManager = getWorkManager();

        // Begin work chain
        workManager
                .beginUniqueWork(workerName, ExistingWorkPolicy.REPLACE, scanRequest)
                .then(downloadRequest)
                .enqueue();

        LiveData<WorkInfo> workInfoLiveData = workManager.getWorkInfoByIdLiveData(downloadRequest.getId());

        workInfoLiveData.observeForever(workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                // Trigger the callback if it is not null
                if (callback != null) {
                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        callback.onDownloadCompleted("success");
                    } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                        callback.onDownloadCompleted("fail");
                    }
                }
            }
        });
    }

    // Define your callback interface with nullable methods
    public interface DownloadCallback {
        void onDownloadCompleted(String status);
    }

    public void startCheckDownloadedFileChainWorker(String filePath) {

        OneTimeWorkRequest fileRequest = getFileUploadRequest();
        OneTimeWorkRequest checkRequest = getCheckDownloadedFileRequest(filePath);

        String workerName = DownloadedFileMonitorWorker.class.getSimpleName();

        getWorkManager().beginUniqueWork(workerName, ExistingWorkPolicy.REPLACE, checkRequest)
                .then(fileRequest)
                .enqueue();
    }

    private OneTimeWorkRequest getCheckDownloadedFileRequest(String filePath) {
        Data data = new Data.Builder()
                .putString(DownloadedFileMonitorWorker.FILE_CHANGE_KEY, filePath)
                .build();

        return oneTimeRequestBuilder(DownloadedFileMonitorWorker.class)
                .setInputData(data)
                .build();
    }


    public void cancelDownloadWorker() {
        cancelById(DownloadWorker.UID);
        cancelById(DownloadedFileMonitorWorker.UID);
        cancelById(DownloadFileScanWorker.UID);
    }

    public void cancelAllJobs() {
        getWorkManager().cancelAllWork();
    }


}
