package dts.rayafile.com.framework.notification;

import android.content.Context;
import android.content.Intent;

import dts.rayafile.com.R;
import dts.rayafile.com.framework.notification.base.BaseTransferNotificationHelper;
import dts.rayafile.com.framework.notification.base.NotificationUtils;

public class FolderBackupScanNotificationHelper extends BaseTransferNotificationHelper {
    public FolderBackupScanNotificationHelper(Context context) {
        super(context);
    }

    @Override
    public Intent getTransferIntent() {
        return null;
    }

    @Override
    public String getDefaultTitle() {
        return context.getString(R.string.settings_folder_backup_info_title);
    }

    @Override
    public String getDefaultSubtitle() {
        return context.getString(R.string.is_scanning);
    }

    @Override
    public int getMaxProgress() {
        return 0;
    }

    @Override
    public String getChannelId() {
        return NotificationUtils.NOTIFICATION_CHANNEL_TRANSFER;
    }

    @Override
    public int getNotificationId() {
        return NotificationUtils.NOTIFICATION_ID_UPLOAD_FOLDER_SCAN;
    }
}
