package dts.rayafile.com.ui.dialog_fragment.viewmodel;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;

import com.blankj.utilcode.util.CollectionUtils;
import dts.rayafile.com.framework.data.db.AppDatabase;
import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.framework.data.model.dirents.DeleteDirentModel;
import dts.rayafile.com.enums.TransferAction;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.ui.dialog_fragment.DialogService;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class DeleteDirsViewModel extends BaseViewModel {
    private final MutableLiveData<Boolean> ActionLiveData = new MutableLiveData<>();

    public MutableLiveData<Boolean> getActionLiveData() {
        return ActionLiveData;
    }

    public void delete(List<String> direntIds, boolean isDeleteLocalFile) {
        Single<List<DirentModel>> dSingle = AppDatabase.getInstance().direntDao().getListByIdsAsync(direntIds);
        addSingleDisposable(dSingle, new Consumer<List<DirentModel>>() {
            @Override
            public void accept(List<DirentModel> dirents) throws Exception {
                deleteDirents(dirents, isDeleteLocalFile);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);
                getActionLiveData().setValue(true);

                String string = getErrorMsgByThrowable(throwable);
                DeleteDirentModel d = new DeleteDirentModel();
                d.error_msg = string;
            }
        });
    }

    private void deleteDirents(List<DirentModel> dirents, boolean isDeleteLocalFile) {
        getRefreshLiveData().setValue(true);

        Flowable<DeleteDirentModel> flowable;
        if (!isDeleteLocalFile) {
            flowable = Flowable.fromIterable(dirents)
                    .flatMapSingle(new Function<DirentModel, SingleSource<DeleteDirentModel>>() {
                        @Override
                        public SingleSource<DeleteDirentModel> apply(DirentModel dirent) throws Exception {
                            return HttpIO.getCurrentInstance().execute(DialogService.class).deleteDirent(dirent.repo_id, dirent.type, dirent.full_path);
                        }
                    });
        } else {
            flowable = Flowable.fromIterable(dirents)
                    .flatMapSingle(new Function<DirentModel, SingleSource<DeleteDirentModel>>() {
                        @Override
                        public SingleSource<DeleteDirentModel> apply(DirentModel dirent) throws Exception {
                            Single<List<FileTransferEntity>> transferSingle = AppDatabase.getInstance()
                                    .fileTransferDAO()
                                    .getListByFullPathsAsync(dirent.repo_id, CollectionUtils.newArrayList(dirent.full_path), TransferAction.DOWNLOAD);
                            return transferSingle.flatMap(new Function<List<FileTransferEntity>, SingleSource<FileTransferEntity>>() {
                                @Override
                                public SingleSource<FileTransferEntity> apply(List<FileTransferEntity> fileTransferEntities) throws Exception {
                                    return CollectionUtils.isEmpty(fileTransferEntities) ?
                                            Single.just(new FileTransferEntity()) :
                                            Single.just(fileTransferEntities.get(0));
                                }
                            }).flatMap(new Function<FileTransferEntity, SingleSource<String>>() {
                                @Override
                                public SingleSource<String> apply(FileTransferEntity fileTransferEntity) throws Exception {

                                    if (TextUtils.isEmpty(fileTransferEntity.uid)) {
                                        return Single.just("");
                                    }

                                    return AppDatabase.getInstance().fileTransferDAO().deleteOneAsync(fileTransferEntity)
                                            .flatMap(new Function<Integer, SingleSource<String>>() {
                                                @Override
                                                public SingleSource<String> apply(Integer integer) throws Exception {
                                                    return Single.just(fileTransferEntity.target_path);
                                                }
                                            });
                                }
                            }).flatMap(new Function<String, SingleSource<DeleteDirentModel>>() {
                                @Override
                                public SingleSource<DeleteDirentModel> apply(String filePath) throws Exception {

                                    if (!TextUtils.isEmpty(filePath)) {
                                        File file = new File(filePath);
                                        if (file.exists()) {
                                            Files.delete(file.toPath());
                                        }
                                    }

                                    return HttpIO.getCurrentInstance().execute(DialogService.class).deleteDirent(dirent.repo_id, dirent.type, dirent.full_path);
                                }
                            });
                        }
                    });
        }

        addFlowableDisposable(flowable, new Consumer<DeleteDirentModel>() {
            @Override
            public void accept(DeleteDirentModel deleteDirentModel) throws Exception {
                SLogs.d(deleteDirentModel.toString());
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);
                getActionLiveData().setValue(true);

                String string = getErrorMsgByThrowable(throwable);
                DeleteDirentModel d = new DeleteDirentModel();
                d.error_msg = string;

            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                getRefreshLiveData().setValue(false);
                getActionLiveData().setValue(true);
            }
        });

    }
}