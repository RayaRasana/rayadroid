package dts.rayafile.com.framework.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.framework.util.SLogs;

import okhttp3.Request;

public abstract class BaseWorker extends Worker {
    private final Account currentAccount;

    protected BaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        currentAccount = SupportAccountManager.getInstance().getCurrentAccount();
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }
}
