package dts.rayafile.com.ui.settings;

import android.content.Context;

import androidx.work.WorkInfo;

import com.blankj.utilcode.util.CollectionUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.AccountInfo;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.framework.data.ServerInfo;
import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.enums.TransferAction;
import dts.rayafile.com.enums.TransferDataSource;
import dts.rayafile.com.enums.TransferStatus;
import dts.rayafile.com.framework.data.model.server.ServerInfoModel;
import dts.rayafile.com.framework.datastore.StorageManager;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.framework.worker.upload.UploadFolderFileAutomaticallyWorker;
import dts.rayafile.com.framework.worker.upload.UploadMediaFileAutomaticallyWorker;
import dts.rayafile.com.preferences.Settings;
import dts.rayafile.com.ui.account.AccountService;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.ui.main.MainService;

import org.apache.commons.io.FileUtils;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;

public class SettingsFragmentViewModel extends BaseViewModel {
    public void getAccountInfo() {
        getRefreshLiveData().setValue(true);

        Single<ServerInfoModel> single1 = HttpIO.getCurrentInstance().execute(MainService.class).getServerInfo();
        Single<AccountInfo> single2 = HttpIO.getCurrentInstance().execute(AccountService.class).getAccountInfo();

        Single<AccountInfo> single = Single.zip(single1, single2, new BiFunction<ServerInfoModel, AccountInfo, AccountInfo>() {
            @Override
            public AccountInfo apply(ServerInfoModel serverInfoModel, AccountInfo accountInfo) throws Exception {

                Account account = SupportAccountManager.getInstance().getCurrentAccount();
                if (account == null) {
                    return accountInfo;
                }

                accountInfo.setServer(HttpIO.getCurrentInstance().getServerUrl());

                ServerInfo serverInfo1 = new ServerInfo(account.server, serverInfoModel.version, serverInfoModel.getFeaturesString(), serverInfoModel.encrypted_library_version);
                SupportAccountManager.getInstance().setServerInfo(account, serverInfo1);

                return accountInfo;
            }
        });

        addSingleDisposable(single, new Consumer<AccountInfo>() {
            @Override
            public void accept(AccountInfo accountInfo) throws Exception {
                getRefreshLiveData().setValue(false);

                // fixme ?
                Settings.USER_INFO.putValue("");
                Settings.SPACE_INFO.putValue("");
                Settings.USER_SERVER_INFO.putValue("");
                Settings.USER_INFO.putValue(accountInfo.getName());
                Settings.USER_SERVER_INFO.putValue(accountInfo.getServer());
                Settings.SPACE_INFO.putValue(accountInfo.getSpaceUsed());
            }
        });
    }

    public void countFolderBackupPendingList(Context context) {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            return;
        }

        Single<Integer> folderBackupInProgressCountSingle = AppDatabase
                .getInstance()
                .fileTransferDAO()
                .getCount(account.getSignature(),
                        TransferAction.UPLOAD,
                        TransferDataSource.FOLDER_BACKUP,
                        CollectionUtils.newArrayList(TransferStatus.IN_PROGRESS, TransferStatus.WAITING));
        addSingleDisposable(folderBackupInProgressCountSingle, new Consumer<Integer>() {
            @Override
            public void accept(Integer s) throws Exception {
                if (s == 0) {
                    Settings.FOLDER_BACKUP_STATE.putValue(context.getString(R.string.folder_backup_waiting_state));
                } else {
                    WorkInfo workInfo = BackgroundJobManagerImpl.getInstance().getWorkInfoById(UploadFolderFileAutomaticallyWorker.UID);
                    if (workInfo != null && WorkInfo.State.ENQUEUED == workInfo.getState()) {
                        Settings.FOLDER_BACKUP_STATE.putValue(context.getString(R.string.waiting));
                    } else {
                        Settings.FOLDER_BACKUP_STATE.putValue(String.valueOf(s));
                    }
                }
            }
        });
    }

    public void countAlbumBackupPendingList(Context context) {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            return;
        }

        Single<Integer> folderBackupInProgressCountSingle = AppDatabase
                .getInstance()
                .fileTransferDAO()
                .getCount(account.getSignature(),
                        TransferAction.UPLOAD,
                        TransferDataSource.ALBUM_BACKUP,
                        CollectionUtils.newArrayList(TransferStatus.IN_PROGRESS, TransferStatus.WAITING));

        addSingleDisposable(folderBackupInProgressCountSingle, new Consumer<Integer>() {
            @Override
            public void accept(Integer s) {
                if (s == 0) {
                    Settings.ALBUM_BACKUP_STATE.putValue(context.getString(R.string.settings_cuc_finish_title));
                } else {
                    WorkInfo workInfo = BackgroundJobManagerImpl.getInstance().getWorkInfoById(UploadMediaFileAutomaticallyWorker.UID);
                    if (workInfo != null && WorkInfo.State.ENQUEUED == workInfo.getState()) {
                        Settings.ALBUM_BACKUP_STATE.putValue(context.getString(R.string.waiting));
                    } else {
                        Settings.ALBUM_BACKUP_STATE.putValue(String.valueOf(s));
                    }
                }
            }
        });
    }

    public void calculateCacheSize() {
        Single<String> single = Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> emitter) throws Exception {
                long l = StorageManager.getInstance().getUsedSpace();

                String total = FileUtils.byteCountToDisplaySize(l);
                emitter.onSuccess(total);
            }
        });
        addSingleDisposable(single, new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                Settings.CACHE_SIZE.putValue(s);
            }
        });

    }
}
