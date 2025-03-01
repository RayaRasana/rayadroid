package dts.rayafile.com.ui.transfer_list;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.WorkInfo;

import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dts.rayafile.com.R;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.enums.TransferAction;
import dts.rayafile.com.enums.TransferDataSource;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.framework.worker.TransferEvent;
import dts.rayafile.com.framework.worker.TransferWorker;
import dts.rayafile.com.framework.worker.upload.UploadFileManuallyWorker;
import dts.rayafile.com.framework.worker.upload.UploadFolderFileAutomaticallyWorker;
import dts.rayafile.com.framework.worker.upload.UploadMediaFileAutomaticallyWorker;

import java.util.List;

import io.reactivex.functions.Consumer;

public class UploadListFragment extends TransferListFragment {

    public static UploadListFragment newInstance() {

        Bundle args = new Bundle();

        UploadListFragment fragment = new UploadListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public TransferAction getTransferAction() {
        return TransferAction.UPLOAD;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initWorkerListener();

    }

    private void initWorkerListener() {
        BackgroundJobManagerImpl.getInstance().getWorkManager()
                .getWorkInfoByIdLiveData(UploadFolderFileAutomaticallyWorker.UID)
                .observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        doWorkInfoLiveData(TransferDataSource.FOLDER_BACKUP, workInfo);
                    }
                });

        BackgroundJobManagerImpl.getInstance().getWorkManager()
                .getWorkInfoByIdLiveData(UploadFileManuallyWorker.UID)
                .observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        doWorkInfoLiveData(TransferDataSource.FILE_BACKUP, workInfo);
                    }
                });

        BackgroundJobManagerImpl.getInstance().getWorkManager()
                .getWorkInfoByIdLiveData(UploadMediaFileAutomaticallyWorker.UID)
                .observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        doWorkInfoLiveData(TransferDataSource.ALBUM_BACKUP, workInfo);
                    }
                });
    }

    private String lastTransferId = null;

    private void doWorkInfoLiveData(TransferDataSource dataSource, WorkInfo workInfo) {
        if (workInfo == null) {
            return;
        }

        Data outData = workInfo.getOutputData();
        String outEvent = outData.getString(TransferWorker.KEY_DATA_STATUS);
        String outExceptionMsg = outData.getString(TransferWorker.KEY_DATA_RESULT);

        if (TransferEvent.EVENT_FINISH.equals(outEvent)) {
//            if (!TextUtils.isEmpty(outExceptionMsg)){
//            }else {
//            }
            refreshData();
            return;
        }

        Data progressData = workInfo.getProgress();
        String progressEvent = progressData.getString(TransferWorker.KEY_DATA_STATUS);

        if (TransferEvent.EVENT_FILE_IN_TRANSFER.equals(progressEvent)) {

            String transferId = progressData.getString(TransferWorker.KEY_TRANSFER_ID);
            String fileName = progressData.getString(TransferWorker.KEY_TRANSFER_NAME);
            int percent = progressData.getInt(TransferWorker.KEY_TRANSFER_PROGRESS, 0);
            long transferredSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TRANSFERRED_SIZE, 0);
            long totalSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TOTAL_SIZE, 0);

            SLogs.d("upload: " + fileName + ", percent：" + percent + ", total_size：" + totalSize + ", dataSource: " + dataSource);

            if (TextUtils.equals(transferId, lastTransferId)) {
                notifyProgressById(transferId, transferredSize, percent, progressEvent);
            } else {
                lastTransferId = transferId;
            }

        } else if (TransferEvent.EVENT_FILE_TRANSFER_SUCCESS.equals(progressEvent)) {
            String transferId = progressData.getString(TransferWorker.KEY_TRANSFER_ID);
            String fileName = progressData.getString(TransferWorker.KEY_TRANSFER_NAME);
            long transferredSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TRANSFERRED_SIZE, 0);
            long totalSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TOTAL_SIZE, 0);

            SLogs.d("upload finish: " + fileName + ", total_size：" + totalSize + ", dataSource: " + dataSource);

            notifyProgressById(transferId, transferredSize, 100, progressEvent);

        } else if (TransferEvent.EVENT_FILE_TRANSFER_FAILED.equals(progressEvent)) {
            String transferId = progressData.getString(TransferWorker.KEY_TRANSFER_ID);
            String fileName = progressData.getString(TransferWorker.KEY_TRANSFER_NAME);
            long transferredSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TRANSFERRED_SIZE, 0);
            long totalSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TOTAL_SIZE, 0);

            SLogs.d("upload failed: " + fileName + ", total_size：" + totalSize + ", dataSource: " + dataSource);

            notifyProgressById(transferId, transferredSize, 0, progressEvent);
        }

    }

    @Override
    public void deleteSelectedItems(List<FileTransferEntity> list) {
        showConfirmDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                BackgroundJobManagerImpl.getInstance().cancelFolderBackupWorker();

                getViewModel().removeSpecialUploadListTask(list, new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        //todo 检查此处逻辑
//                        BackgroundJobManagerImpl.getInstance().startFolderUploadWorker();

                        //You never know which item a user will select, so we need to remove them one by one, and then resort.
                        for (FileTransferEntity fileTransferEntity : list) {
                            removeSpecialEntity(fileTransferEntity.uid);
                        }

                        getViewModel().getShowLoadingDialogLiveData().setValue(false);

                        ToastUtils.showLong(R.string.deleted);
                    }
                });

                dialog.dismiss();
            }
        });
    }


    @Override
    public void restartSelectedItems(List<FileTransferEntity> list) {
        getViewModel().restartUpload(list, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {

                //todo 检查此处逻辑
                BackgroundJobManagerImpl.getInstance().startFolderBackupWorkerChain(true);
                BackgroundJobManagerImpl.getInstance().startMediaBackupWorkerChain(true);
            }
        });
    }

    /**
     * cancel all download tasks
     */
    public void cancelAllTasks() {

        BackgroundJobManagerImpl.getInstance().cancelFolderBackupWorker();
        BackgroundJobManagerImpl.getInstance().cancelMediaBackupWorker();

        getViewModel().cancelAllUploadTask(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                ToastUtils.showLong(R.string.upload_cancelled);

                refreshData();
            }
        });
    }


    /**
     * remove all download tasks
     */
    public void removeAllTasks() {
        showConfirmDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
                BackgroundJobManagerImpl.getInstance().cancelFolderBackupWorker();

                //
                getViewModel().removeAllUploadTask(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        ToastUtils.showLong(R.string.upload_cancelled);

                        refreshData();
                    }
                });
            }
        });

    }

    private void showConfirmDialog(DialogInterface.OnClickListener listener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.delete);
        builder.setMessage(R.string.delete_records);
        builder.setPositiveButton(R.string.ok, listener);

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}

