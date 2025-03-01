package dts.rayafile.com.ui.transfer_list;

import androidx.lifecycle.MutableLiveData;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.FileUtils;
import dts.rayafile.com.SeafException;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.enums.TransferAction;
import dts.rayafile.com.enums.TransferDataSource;
import dts.rayafile.com.enums.TransferResult;
import dts.rayafile.com.enums.TransferStatus;
import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.framework.worker.upload.UploadFileManuallyWorker;
import dts.rayafile.com.framework.worker.upload.UploadFolderFileAutomaticallyWorker;
import dts.rayafile.com.framework.worker.upload.UploadMediaFileAutomaticallyWorker;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class TransferListViewModel extends BaseViewModel {

    private Account account;
    private final MutableLiveData<List<FileTransferEntity>> mTransferListLiveData = new MutableLiveData<>();

    public MutableLiveData<List<FileTransferEntity>> getTransferListLiveData() {
        return mTransferListLiveData;
    }

    public void loadData(TransferAction transferAction, int pageIndex, int pageSize, boolean isShowRefresh) {
        if (isShowRefresh) {
            getRefreshLiveData().setValue(true);
        }

        if (account == null) {
            account = SupportAccountManager.getInstance().getCurrentAccount();
        }

        if (account == null) {
            getRefreshLiveData().setValue(false);
            return;
        }

        int offset = (pageIndex - 1) * pageSize;

        Single<List<FileTransferEntity>> single;
        if (TransferAction.UPLOAD == transferAction) {
            single = AppDatabase
                    .getInstance()
                    .fileTransferDAO()
                    .getPageUploadListAsync(account.getSignature(), pageSize, offset);
        } else {
            single = AppDatabase
                    .getInstance()
                    .fileTransferDAO()
                    .getPageDownloadListAsync(account.getSignature(), pageSize, offset);
        }

        addSingleDisposable(single, list -> {
            getTransferListLiveData().setValue(list);

            if (isShowRefresh) {
                getRefreshLiveData().setValue(false);
            }
        });
    }


    public void deleteTransferData(FileTransferEntity fileTransferEntity, boolean isDeleteLocalFile, TransferAction transferAction, Consumer<Boolean> consumer) {

        Single<Boolean> single = Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {

                if (TransferDataSource.DOWNLOAD == fileTransferEntity.data_source) {
                    if (fileTransferEntity.transfer_status == TransferStatus.IN_PROGRESS) {
                        BackgroundJobManagerImpl.getInstance().cancelDownloadWorker();
                    }

                    if (isDeleteLocalFile) {
                        FileUtils.delete(fileTransferEntity.target_path);
                    }

                    AppDatabase.getInstance().fileTransferDAO().deleteOne(fileTransferEntity);

                    BackgroundJobManagerImpl.getInstance().startDownloadChainWorker();


                } else if (TransferDataSource.FILE_BACKUP == fileTransferEntity.data_source) {
                    if (fileTransferEntity.transfer_status == TransferStatus.IN_PROGRESS) {
                        BackgroundJobManagerImpl.getInstance().cancelById(UploadFileManuallyWorker.UID);
                    }

                    AppDatabase.getInstance().fileTransferDAO().deleteOne(fileTransferEntity);
                    FileUtils.delete(fileTransferEntity.full_path);

                    BackgroundJobManagerImpl.getInstance().startFileManualUploadWorker();

                } else if (TransferDataSource.FOLDER_BACKUP == fileTransferEntity.data_source) {
                    //
                    BackgroundJobManagerImpl.getInstance().cancelById(UploadFolderFileAutomaticallyWorker.UID);

                    //Delete data logically, not physically.
                    fileTransferEntity.data_status = -1;
                    fileTransferEntity.result = SeafException.USER_CANCELLED_EXCEPTION.getMessage();
                    fileTransferEntity.transfer_status = TransferStatus.CANCELLED;
                    fileTransferEntity.transferred_size = 0;

                    AppDatabase.getInstance().fileTransferDAO().update(fileTransferEntity);

                    BackgroundJobManagerImpl.getInstance().startFolderBackupWorkerChain(true);

                } else if (TransferDataSource.ALBUM_BACKUP == fileTransferEntity.data_source) {
                    BackgroundJobManagerImpl.getInstance().cancelById(UploadMediaFileAutomaticallyWorker.UID);

                    //Delete data logically, not physically.
                    fileTransferEntity.data_status = -1;
                    fileTransferEntity.result = SeafException.USER_CANCELLED_EXCEPTION.getMessage();
                    fileTransferEntity.transfer_status = TransferStatus.CANCELLED;
                    fileTransferEntity.transferred_size = 0;

                    AppDatabase.getInstance().fileTransferDAO().update(fileTransferEntity);

                    BackgroundJobManagerImpl.getInstance().startMediaBackupWorkerChain(true);
                }
                emitter.onSuccess(true);
            }
        });

        addSingleDisposable(single, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                consumer.accept(true);
            }
        });
    }

    public void cancelAllUploadTask(Consumer<Boolean> consumer) {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        List<TransferDataSource> dataSources = CollectionUtils.newArrayList(TransferDataSource.ALBUM_BACKUP, TransferDataSource.FILE_BACKUP, TransferDataSource.FOLDER_BACKUP);
        Completable completable = AppDatabase.getInstance().fileTransferDAO().cancelAllByDataSource(account.getSignature(), dataSources, SeafException.USER_CANCELLED_EXCEPTION.getMessage());
        addCompletableDisposable(completable, new Action() {
            @Override
            public void run() throws Exception {
                consumer.accept(true);
            }
        });
    }

    public void cancelAllDownloadTask(Consumer<Boolean> consumer) {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            return;
        }

        List<TransferDataSource> dataSources = CollectionUtils.newArrayList(TransferDataSource.DOWNLOAD);
        Completable completable = AppDatabase.getInstance().fileTransferDAO().cancelAllByDataSource(account.getSignature(), dataSources, SeafException.USER_CANCELLED_EXCEPTION.getMessage());
        addCompletableDisposable(completable, new Action() {
            @Override
            public void run() throws Exception {
                consumer.accept(true);
            }
        });
    }


    public void removeSpecialDownloadListTask(List<FileTransferEntity> list, boolean isDeleteLocalFile, Consumer<Boolean> consumer) {
        Single<Boolean> single = Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {

                for (FileTransferEntity entity : list) {
                    //delete record
                    AppDatabase.getInstance().fileTransferDAO().deleteOne(entity);

                    if (isDeleteLocalFile) {
                        FileUtils.delete(entity.target_path);
                    }
                    SLogs.d("deleted : " + entity.target_path);
                }

                emitter.onSuccess(true);
            }
        });

        addSingleDisposable(single, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (consumer != null) {
                    consumer.accept(true);
                }
            }
        });
    }

    public void removeAllDownloadTask(Consumer<Boolean> consumer, boolean isDeleteLocalFile) {
        getRefreshLiveData().setValue(true);

        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            getRefreshLiveData().setValue(false);
            return;
        }

        List<TransferDataSource> features = CollectionUtils.newArrayList(TransferDataSource.DOWNLOAD);
        //query all data, including deleted data, based on different users
        Single<List<FileTransferEntity>> single = AppDatabase.getInstance().fileTransferDAO().getListByDataSourceAsync(account.getSignature(), features);

        Single<Boolean> single1 = single.flatMap(new Function<List<FileTransferEntity>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> apply(List<FileTransferEntity> transferList) throws Exception {
                return Single.create(new SingleOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {
                        if (isDeleteLocalFile) {
                            for (FileTransferEntity entity : transferList) {
                                FileUtils.delete(entity.target_path);
                                SLogs.d("deleted : " + entity.target_path);
                            }
                        }

                        AppDatabase.getInstance().fileTransferDAO().deleteAllByAction(account.getSignature(), TransferAction.DOWNLOAD);

                        emitter.onSuccess(true);
                    }
                });
            }
        });

        addSingleDisposable(single1, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean b) throws Exception {
                if (consumer != null) {
                    consumer.accept(true);
                }
            }
        });
    }

    public void removeSpecialUploadListTask(List<FileTransferEntity> list, Consumer<Boolean> consumer) {
        Single<Boolean> single = Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {

                for (FileTransferEntity entity : list) {
                    entity.data_status = -1;
                    entity.transfer_status = TransferStatus.CANCELLED;
                    entity.result = SeafException.USER_CANCELLED_EXCEPTION.getMessage();
                    entity.transferred_size = 0;

                    AppDatabase.getInstance().fileTransferDAO().update(entity);
                }

                emitter.onSuccess(true);
            }
        });

        addSingleDisposable(single, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (consumer != null) {
                    consumer.accept(true);
                }
            }
        });
    }

    public void removeAllUploadTask(Consumer<Boolean> consumer) {
        getShowLoadingDialogLiveData().setValue(true);

        Account account = SupportAccountManager.getInstance().getCurrentAccount();

        Completable completable = AppDatabase.getInstance().fileTransferDAO().removeAllUploadByAccount(account.getSignature(), SeafException.USER_CANCELLED_EXCEPTION.getMessage());
        addCompletableDisposable(completable, new Action() {
            @Override
            public void run() throws Exception {
                if (consumer != null) {
                    consumer.accept(true);
                }

                getShowLoadingDialogLiveData().setValue(false);
            }
        });

    }

    public void restartSpecialStatusTask(TransferAction transferAction, TransferStatus transferStatus, Consumer<Boolean> consumer) {
        getShowLoadingDialogLiveData().setValue(true);

        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            getShowLoadingDialogLiveData().setValue(false);
            return;
        }

        Single<List<FileTransferEntity>> single = AppDatabase.getInstance().fileTransferDAO().getByActionAndStatusAsync(account.getSignature(), transferAction, transferStatus);
        Single<Boolean> single1 = single.flatMap(new Function<List<FileTransferEntity>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> apply(List<FileTransferEntity> list) throws Exception {

                if (CollectionUtils.isEmpty(list)) {
                    return Single.just(false);
                }

                return Single.create(new SingleOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {

                        for (FileTransferEntity entity : list) {
                            if (transferAction == TransferAction.DOWNLOAD) {
                                FileUtils.delete(entity.target_path);
                                SLogs.d("deleted : " + entity.target_path);
                            }

                            entity.transfer_status = TransferStatus.WAITING;
                            entity.result = null;
                            entity.transferred_size = 0;
                            entity.action_end_at = 0;

                            AppDatabase.getInstance().fileTransferDAO().update(entity);
                        }
                        emitter.onSuccess(true);
                    }
                });
            }
        });

        addSingleDisposable(single1, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (consumer != null) {
                    consumer.accept(aBoolean);
                }
                getShowLoadingDialogLiveData().setValue(false);
            }
        });

    }

    public void restartUpload(List<FileTransferEntity> list, Consumer<Boolean> consumer) {
        Single<Boolean> single = Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {

                for (FileTransferEntity entity : list) {
                    if (entity.transfer_status == TransferStatus.WAITING) {
                        continue;
                    }

                    entity.transfer_status = TransferStatus.WAITING;
                    entity.result = null;
                    entity.transferred_size = 0;
                    entity.action_end_at = 0;

                    AppDatabase.getInstance().fileTransferDAO().update(entity);
                }

                emitter.onSuccess(true);
            }
        });
        addSingleDisposable(single, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (consumer != null) {
                    consumer.accept(true);
                }
            }
        });

    }

    public void restartDownload(List<FileTransferEntity> list, Consumer<Boolean> consumer) {
        Single<Boolean> single = Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(SingleEmitter<Boolean> emitter) throws Exception {

                for (FileTransferEntity entity : list) {
                    if (entity.transfer_status == TransferStatus.WAITING) {
                        continue;
                    }

                    if (entity.transfer_action == TransferAction.DOWNLOAD) {
                        FileUtils.delete(entity.target_path);
                        SLogs.d("deleted : " + entity.target_path);
                    }

                    entity.transfer_status = TransferStatus.WAITING;
                    entity.result = null;
                    entity.transferred_size = 0;
                    entity.action_end_at = 0;

                    AppDatabase.getInstance().fileTransferDAO().update(entity);
                }

                emitter.onSuccess(true);
            }
        });

        addSingleDisposable(single, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (consumer != null) {
                    consumer.accept(true);
                }
            }
        });
    }
}
