package dts.rayafile.com.framework.notification;

import static dts.rayafile.com.framework.notification.base.NotificationUtils.NOTIFICATION_MESSAGE_KEY;
import static dts.rayafile.com.framework.notification.base.NotificationUtils.NOTIFICATION_OPEN_UPLOAD_TAB;

import android.content.Context;
import android.content.Intent;

import androidx.work.ForegroundInfo;

import dts.rayafile.com.R;
import dts.rayafile.com.framework.notification.base.BaseTransferNotificationHelper;
import dts.rayafile.com.framework.notification.base.NotificationUtils;
import dts.rayafile.com.ui.transfer_list.TransferActivity;

public class FileBackupNotificationHelper extends BaseTransferNotificationHelper {
    public FileBackupNotificationHelper(Context context) {
        super(context);
    }

    @Override
    public Intent getTransferIntent() {
        Intent dIntent = new Intent(context, TransferActivity.class);
        dIntent.putExtra(NOTIFICATION_MESSAGE_KEY, NOTIFICATION_OPEN_UPLOAD_TAB);
        return dIntent;
    }

    @Override
    public String getDefaultTitle() {
        return context.getString(R.string.settings_file_backup_info_title);
    }

    @Override
    public String getDefaultSubtitle() {
        return context.getString(R.string.uploading);
    }

    @Override
    public int getMaxProgress() {
        return 100;
    }

    @Override
    public String getChannelId() {
        return NotificationUtils.NOTIFICATION_CHANNEL_TRANSFER;
    }

    @Override
    public int getNotificationId() {
        return NotificationUtils.NOTIFICATION_ID_UPLOAD_FILE;
    }
}
