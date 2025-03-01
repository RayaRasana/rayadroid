package dts.rayafile.com.framework.file_monitor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;

import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.framework.datastore.sp_livedata.AlbumBackupSharePreferenceHelper;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.ui.camera_upload.CameraUploadManager;

/**
 * This service monitors the media provider content provider for new images/videos.
 * <p>
 * If new content appears, this service will get notified and send a syncRequest to the MediaSyncProvider.
 * <p>
 * This service is always running, even if camera upload is not active.
 * However, it will only register it's ContentObservers if Camera Upload is enabled in Seadroid.
 */
public class MediaContentObserver extends ContentObserver {
    private final Context mContext;
    private final Uri mImagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private final Uri mVideosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public MediaContentObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        String newVersion = MediaStore.getVersion(SeadroidApplication.getAppContext());
        String lastVersion = AlbumBackupSharePreferenceHelper.readLastMediaVersion();

        SLogs.e("media store：newVersion -> " + newVersion + ", lastVersion -> " + lastVersion);
        SLogs.e("A new file is detected and the Media task begins");

        if (TextUtils.equals(newVersion, lastVersion)) {
            CameraUploadManager.getInstance().performSync(false);
        } else {
            //
            AlbumBackupSharePreferenceHelper.writeLastMediaVersion(newVersion);
            //
            CameraUploadManager.getInstance().performSync(true);
        }
    }

    public void register() {
        if (mContext != null) {
            ContentResolver contentResolver = mContext.getContentResolver();
            contentResolver.registerContentObserver(mImagesUri, true, this);
            contentResolver.registerContentObserver(mVideosUri, true, this);
        }
    }

    public void unregister() {
        if (mContext != null) {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }
}
