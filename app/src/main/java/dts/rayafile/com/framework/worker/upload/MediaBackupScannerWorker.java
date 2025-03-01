package dts.rayafile.com.framework.worker.upload;

import android.app.ForegroundServiceStartNotAllowedException;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import dts.rayafile.com.R;
import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.SeafException;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.enums.TransferResult;
import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.enums.TransferDataSource;
import dts.rayafile.com.framework.data.model.repo.DirentWrapperModel;
import dts.rayafile.com.framework.datastore.StorageManager;
import dts.rayafile.com.framework.datastore.sp_livedata.AlbumBackupSharePreferenceHelper;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.notification.AlbumBackupScanNotificationHelper;
import dts.rayafile.com.framework.util.HttpUtils;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.util.Utils;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.framework.worker.TransferEvent;
import dts.rayafile.com.framework.worker.TransferWorker;
import dts.rayafile.com.ui.camera_upload.CameraUploadManager;
import dts.rayafile.com.ui.camera_upload.GalleryBucketUtils;
import dts.rayafile.com.ui.file.FileService;
import dts.rayafile.com.ui.folder_backup.RepoConfig;
import dts.rayafile.com.ui.repo.RepoService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.RequestBody;
import retrofit2.Call;

/**
 * Worker Tag:
 *
 * @see BackgroundJobManagerImpl#TAG_ALL
 * @see BackgroundJobManagerImpl#TAG_TRANSFER
 */
public class MediaBackupScannerWorker extends TransferWorker {
    public static final UUID UID = UUID.nameUUIDFromBytes(MediaBackupScannerWorker.class.getSimpleName().getBytes());

    private final AlbumBackupScanNotificationHelper notificationManager;

    public static final String BASE_DIR = "My Photos";

    private RepoConfig repoConfig;
    private Account account;

    /**
     * key: bucketName
     */
    private final Map<String, List<FileTransferEntity>> pendingUploadMap = new HashMap<>();
    private List<String> bucketIdList;

    public MediaBackupScannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        notificationManager = new AlbumBackupScanNotificationHelper(context);

        account = SupportAccountManager.getInstance().getCurrentAccount();
    }


    private void showNotification() {

        String title = getApplicationContext().getString(R.string.settings_camera_upload_info_title);
        String subTitle = getApplicationContext().getString(R.string.is_scanning);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ForegroundInfo foregroundInfo = notificationManager.getForegroundNotification(title,subTitle);
                showForegroundAsync(foregroundInfo);
            } catch (ForegroundServiceStartNotAllowedException e) {
                SLogs.e(e.getMessage());
            }
        } else {
            ForegroundInfo foregroundInfo = notificationManager.getForegroundNotification(title,subTitle);
            showForegroundAsync(foregroundInfo);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            return Result.success(getOutData());
        }

        boolean isEnable = AlbumBackupSharePreferenceHelper.readBackupSwitch();
        if (!isEnable) {
            SLogs.d("the album scan task was not started, because the switch is off");
            return Result.success(getOutData());
        }

        Account backupAccount = CameraUploadManager.getInstance().getCameraAccount();
        if (backupAccount == null) {
            SLogs.d("the album scan task was not started, because the backup account is null");
            return Result.success(getOutData());
        }

        repoConfig = AlbumBackupSharePreferenceHelper.readRepoConfig();
        if (repoConfig == null || TextUtils.isEmpty(repoConfig.getRepoId())) {
            SLogs.d("the album scan task was not started, because the repoConfig is null");
            return Result.success(getOutData());
        }

        boolean isForce = getInputData().getBoolean(TransferWorker.DATA_FORCE_TRANSFER_KEY, false);
        if (isForce) {
            AlbumBackupSharePreferenceHelper.resetLastScanTime();
        }

        // check
        bucketIdList = AlbumBackupSharePreferenceHelper.readBucketIds();

        showNotification();

        try {
            SLogs.d("MediaSyncWorker start");
            sendEvent(TransferDataSource.ALBUM_BACKUP, TransferEvent.EVENT_SCANNING);

            loadMedia();

        } catch (SeafException | IOException e) {
            SLogs.e("MediaBackupScannerWorker has occurred error", e);
        } finally {
            AlbumBackupSharePreferenceHelper.writeLastScanTime(System.currentTimeMillis());
        }

        //get total count: WAITING, IN_PROGRESS, FAILED
        long totalPendingCount = getCurrentPendingCount(account, TransferDataSource.ALBUM_BACKUP);
        String content = null;
        if (totalPendingCount > 0) {
            boolean isAllowUpload = checkNetworkTypeIfAllowStartUploadWorker();
            if (!isAllowUpload) {
                content = TransferResult.WAITING.name();
            }
        }

        return Result.success(getOutData(content));
    }

    private Data getOutData() {
        return getOutData(null);
    }

    private Data getOutData(String content) {
        return new Data.Builder()
                .putString(TransferWorker.KEY_DATA_STATUS, TransferEvent.EVENT_SCAN_END)
                .putString(TransferWorker.KEY_DATA_SOURCE, TransferDataSource.ALBUM_BACKUP.name())
                .putString(TransferWorker.KEY_DATA_RESULT, content)
                .build();
    }

    private boolean checkNetworkTypeIfAllowStartUploadWorker() {
        boolean isAllowData = AlbumBackupSharePreferenceHelper.readAllowDataPlanSwitch();
        if (isAllowData) {
            return true;
        }

        //如果不允许数据上传，但是当前是数据网络
        return !NetworkUtils.isMobileData();
    }

    private void loadMedia() throws SeafException, IOException {
        if (CollectionUtils.isEmpty(bucketIdList)) {
            List<GalleryBucketUtils.Bucket> allBuckets = GalleryBucketUtils.getMediaBuckets(SeadroidApplication.getAppContext());
            for (GalleryBucketUtils.Bucket bucket : allBuckets) {
                //if user choose to back up the default album, only the "Camera" folder on phone will be read
                if (bucket.isCameraBucket) {
                    bucketIdList.add(bucket.bucketId);
                }
            }
        }

        Stopwatch stopwatch = Stopwatch.createStarted();

        long lastScanTime = AlbumBackupSharePreferenceHelper.readLastScanTimeMills();
        uploadImages(lastScanTime);

        if (AlbumBackupSharePreferenceHelper.readAllowVideoSwitch()) {
            uploadVideos(lastScanTime);
        }

        stopwatch.stop();
        long diff = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        SLogs.e("album backup scan time：" + stopwatch);
        long now = System.currentTimeMillis();
        AlbumBackupSharePreferenceHelper.writeLastScanTime(now - diff);

        if (pendingUploadMap.isEmpty()) {
            SLogs.d("UploadMediaSyncWorker no need to upload");
            return;
        }

        // create directories for media buckets
        createDirectories();
    }

    /**
     * If lastScanTimeLong is not 0, the time range from the previous 5 minutes of lastScanTimeLong to the now( >= ?)
     *
     * @param lastScanTimeLong mills
     */
    private void uploadImages(long lastScanTimeLong) {
        if (isStopped())
            return;

        if (bucketIdList.isEmpty()) {
            SLogs.d("no media in local storage");
            return;
        }

        String[] selectionArgs = bucketIdList.toArray(new String[]{});
        String selection = MediaStore.Images.ImageColumns.BUCKET_ID + " IN " + varArgs(bucketIdList.size());

        //incremental queries
        if (lastScanTimeLong > 0) {
            //query
            selection += " and " + MediaStore.Images.Media.DATE_ADDED + " >= ? "; // If it's >=, might get some duplicate data
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = String.valueOf(lastScanTimeLong / 1000);
        }

        Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.ImageColumns.DATE_ADDED
                },
                selection,
                selectionArgs,
                MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"
        );

        try {
            if (cursor == null) {
                return;
            }

            SLogs.d("images query count : " + cursor.getCount());
            if (cursor.getCount() == 0) {
                return;
            }

            //iterate
            iterateCursor(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void uploadVideos(long lastScanTimeLong) {
        if (isStopped())
            return;

        if (bucketIdList.isEmpty()) {
            SLogs.d("no media in local storage");
            return;
        }

        String[] selectionArgs = bucketIdList.toArray(new String[]{});
        String selection = MediaStore.Video.VideoColumns.BUCKET_ID + " IN " + varArgs(bucketIdList.size());

        if (lastScanTimeLong > 0) {
            //query
            selection += " and " + MediaStore.Images.Media.DATE_ADDED + " >= ? "; // If it's >=, might get some duplicate data
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = String.valueOf(lastScanTimeLong / 1000);
        }

        Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.ImageColumns.DATE_ADDED
                },
                selection,
                selectionArgs,
                MediaStore.Video.VideoColumns.DATE_ADDED + " DESC"
        );

        try {
            if (cursor == null) {
                return;
            }

            if (cursor.getCount() == 0) {
                return;
            }

            //
            iterateCursor(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    private String varArgs(int count) {
        String[] chars = new String[count];
        Arrays.fill(chars, "?");
        return "( " + Joiner.on(", ").join(chars) + " )";
    }

    /**
     * Iterate through the content provider and upload all files
     *
     * @param cursor
     */
    private void iterateCursor(Cursor cursor) {
        String localCacheAbsPath = StorageManager.getInstance().getMediaDir().getAbsolutePath();

        // load them one by one
        while (!isStopped() && cursor.moveToNext()) {

            int dateAddIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
            long dateAdded = cursor.getLong(dateAddIndex);
            String dateAddedString = TimeUtils.millis2String(dateAdded * 1000, "yyyy-MM-dd HH:mm:ss");

            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            String localPath = cursor.getString(dataIndex);
            if (TextUtils.isEmpty(localPath)) {
                SLogs.i("skip file -> dataIndex: " + dataIndex + ", because it doesn't exist");
                continue;
            }

//            String p = Utils.getRealPathFromURI(SeadroidApplication.getAppContext(), videoUri, media);

            File file = new File(localPath);
            if (!file.exists()) {
                // local file does not exist. some inconsistency in the Media Provider? Ignore and continue
                SLogs.i("skip file -> dateAddedString " + dateAddedString + ", localPath: " + localPath + ", because it doesn't exist");
                continue;
            }

            // Ignore all media by Seafile. We don't want to upload our own cached files.
            if (file.getAbsolutePath().startsWith(localCacheAbsPath)) {
                SLogs.i("skip file -> dateAddedString " + dateAddedString + ", localPath: " + localPath + ", because it's part of the Seadroid cache");
                continue;
            }

            List<FileTransferEntity> transferEntityList = AppDatabase
                    .getInstance()
                    .fileTransferDAO()
                    .getListByFullPathSync(repoConfig.getRepoId(), TransferDataSource.ALBUM_BACKUP, file.getAbsolutePath());


            int bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);
            String bucketName = cursor.getString(bucketColumn);

            FileTransferEntity transferEntity;
            if (!CollectionUtils.isEmpty(transferEntityList)) {
                SLogs.d("skip file -> dateAddedString" + dateAddedString + ", localPath " + localPath + ", because we have uploaded it in the past.");
                transferEntity = transferEntityList.get(0);
            } else {
                transferEntity = convertLocalFilePathToEntity(bucketName, localPath);
                SLogs.d("new file -> dateAddedString " + dateAddedString + ", localPath " + localPath);
            }


            List<FileTransferEntity> needUploadList = pendingUploadMap.getOrDefault(bucketName, null);
            if (needUploadList == null) {
                needUploadList = new ArrayList<>();
            }

            needUploadList.add(transferEntity);
            pendingUploadMap.put(bucketName, needUploadList);
        }
    }

    /**
     * Create all the subdirectories on the server for the buckets that are about to be uploaded.
     */
    private void createDirectories() throws SeafException, IOException {
        Set<String> keys = pendingUploadMap.keySet();

        // create base directory
        createBucketDirectoryIfNecessary("/", BASE_DIR, null);

        for (String bucketName : keys) {
            createBucketDirectoryIfNecessary("/" + BASE_DIR, bucketName, pendingUploadMap.get(bucketName));
        }
    }

    /**
     * Create a directory, rename a file away if necessary,
     *
     * @param parent     parent dir
     * @param bucketName directory to create
     */
    private void createBucketDirectoryIfNecessary(String parent, String bucketName, List<FileTransferEntity> pendingList) throws IOException, SeafException {

        parent = Utils.pathJoin(parent, "/");// / -> /

        DirentWrapperModel direntWrapperModel = getDirentWrapper(repoConfig.getRepoId(), parent);
        if (direntWrapperModel == null) {
            SLogs.e("MediaBackupScannerWorker -> createBucketDirectoryIfNecessary() -> request dirents is null");
            return;
        }


        String curPath = Utils.pathJoin(parent, bucketName);
        boolean found = false;

        //check whether the file in the parent directory contains the bucket name
        List<DirentModel> parentDirentList = direntWrapperModel.dirent_list;
        for (DirentModel dirent : parentDirentList) {
            if (!TextUtils.equals(dirent.name, bucketName)) {
                continue;
            }

            //same folder name
            if (dirent.isDir()) {
                found = true;
            } else {
                //if a file same with the bucketName, rename it
                renameRemoteFile(dirent.name, curPath);
            }

            break;
        }

        //if not, create bucketName dir.
        if (!found) {
            mkdirRemote(repoConfig.getRepoId(), curPath);
        }

        if (CollectionUtils.isEmpty(pendingList)) {
            return;
        }

        //
        checkAndInsert(curPath, pendingList);
    }

    private void checkAndInsert(String parent, List<FileTransferEntity> pendingList) throws IOException {
        DirentWrapperModel direntWrapperModel = getDirentWrapper(repoConfig.getRepoId(), parent);
        if (direntWrapperModel == null) {
            SLogs.e("MediaBackupScannerWorker -> checkAndInsert() -> request dirents is null");
            return;
        }

        List<DirentModel> remoteList = direntWrapperModel.dirent_list;
        List<FileTransferEntity> transferList = CollectionUtils.newArrayList();
        for (FileTransferEntity transferEntity : pendingList) {
            if (CollectionUtils.isEmpty(remoteList)) {
                transferList.add(transferEntity);
                continue;
            }

            //check whether the file in the parent directory contains the bucket name
            DirentModel exitsDirent = null;
            Optional<DirentModel> firstOp = remoteList.stream().filter(f -> f.name.equals(transferEntity.file_name)).findFirst();
            if (firstOp.isPresent()) {
                exitsDirent = firstOp.get();
            }

            if (exitsDirent == null) {
                transferList.add(transferEntity);
                continue;
            }

            if (!TextUtils.equals(transferEntity.file_id, exitsDirent.id)) {
                transferList.add(transferEntity);
            }
        }

        if (!CollectionUtils.isEmpty(transferList)) {
            AppDatabase.getInstance().fileTransferDAO().insertAll(transferList);
        }
    }

    private FileTransferEntity convertLocalFilePathToEntity(String parent, String filePath) {
        File file = new File(filePath);
        return FileTransferEntity.convert2ThisForUploadMediaSyncWorker(account, file, parent, file.lastModified(), null);
    }

    private DirentWrapperModel getDirentWrapper(String repoId, String parentPath) throws IOException {
        //get parent dirent list from remote
        Call<DirentWrapperModel> direntWrapperModelCall = HttpIO.getCurrentInstance().execute(RepoService.class).getDirentsSync(repoId, parentPath);
        retrofit2.Response<DirentWrapperModel> res = direntWrapperModelCall.execute();
        if (!res.isSuccessful()) {
            SLogs.e("MediaBackupScannerWorker -> getDirentWrapper() -> request dirents failed");
            return null;
        }

        DirentWrapperModel tempWrapper = res.body();
        if (tempWrapper == null) {
            SLogs.e("MediaBackupScannerWorker -> getDirentWrapper() -> request dirents is null");
            return null;
        }

        if (!TextUtils.isEmpty(tempWrapper.error_msg)) {
            SLogs.e("MediaBackupScannerWorker -> getDirentWrapper(): " + tempWrapper.error_msg);
            return null;
        }
        return tempWrapper;
    }


    private void renameRemoteFile(String name, String fullPath) throws IOException {
        // there is already a file. move it away.
        String newFilename = getApplicationContext().getString(R.string.camera_sync_rename_file, name);

        Map<String, String> renameMap = new HashMap<>();
        renameMap.put("operation", "rename");
        renameMap.put("newname", newFilename);
        Map<String, RequestBody> requestBodyMap = HttpUtils.generateRequestBody(renameMap);

        retrofit2.Response<String> renameRes = HttpIO.getCurrentInstance()
                .execute(FileService.class)
                .renameFileCall(repoConfig.getRepoId(), fullPath, requestBodyMap)
                .execute();
        if (renameRes.isSuccessful()) {
            SLogs.d("Folder rename success：" + fullPath);
        } else {
            SLogs.d("Folder rename failed：" + fullPath);
        }
    }

}
