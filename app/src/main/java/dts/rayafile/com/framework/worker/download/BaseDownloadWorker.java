package dts.rayafile.com.framework.worker.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import dts.rayafile.com.R;
import dts.rayafile.com.SeafException;
import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.enums.TransferResult;
import dts.rayafile.com.enums.TransferStatus;
import dts.rayafile.com.framework.notification.base.BaseNotification;
import dts.rayafile.com.framework.worker.TransferEvent;
import dts.rayafile.com.framework.worker.TransferWorker;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLHandshakeException;

public abstract class BaseDownloadWorker extends TransferWorker {

    public abstract BaseNotification getNotification();

    public BaseDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
}
