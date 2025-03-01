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

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dts.rayafile.com.R;
import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
import dts.rayafile.com.enums.TransferAction;
import dts.rayafile.com.enums.TransferDataSource;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.framework.worker.download.DownloadWorker;
import dts.rayafile.com.framework.worker.TransferEvent;
import dts.rayafile.com.framework.worker.TransferWorker;

import java.util.List;

import io.reactivex.functions.Consumer;

public class DownloadListFragment extends TransferListFragment {

    public static DownloadListFragment newInstance() {
        Bundle args = new Bundle();
        DownloadListFragment fragment = new DownloadListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public TransferAction getTransferAction() {
        return TransferAction.DOWNLOAD;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initWorkerListener();

    }

    private void initWorkerListener() {
        BackgroundJobManagerImpl.getInstance().getWorkManager()
                .getWorkInfoByIdLiveData(DownloadWorker.UID)
                .observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        doWorkInfoLiveData(TransferDataSource.DOWNLOAD, workInfo);
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

        String exceptionMsg = outData.getString(TransferWorker.KEY_DATA_RESULT);
        if (TransferEvent.EVENT_FINISH.equals(outEvent)) {
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

            SLogs.d("download: " + fileName + ", percent：" + percent + ", total_size：" + totalSize + ", dataSource: " + dataSource);

            if (TextUtils.equals(transferId, lastTransferId)) {
                notifyProgressById(transferId, transferredSize, percent, progressEvent);
            } else {
                lastTransferId = transferId;
            }
        } else if (TransferEvent.EVENT_FILE_TRANSFER_SUCCESS.equals(progressEvent) ||
                TransferEvent.EVENT_FILE_TRANSFER_FAILED.equals(progressEvent)) {

            String transferId = progressData.getString(TransferWorker.KEY_TRANSFER_ID);
            String fileName = progressData.getString(TransferWorker.KEY_TRANSFER_NAME);
            long transferredSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TRANSFERRED_SIZE, 0);
            long totalSize = progressData.getLong(TransferWorker.KEY_TRANSFER_TOTAL_SIZE, 0);

            SLogs.d("download finish: " + fileName + ", total_size：" + totalSize + ", dataSource: " + dataSource);

            notifyProgressById(transferId, transferredSize, 100, progressEvent);

        }

    }

    @Override
    public void deleteSelectedItems(List<FileTransferEntity> list) {
        showDeleteConfirmDialog(list);
    }

    private void showDeleteConfirmDialog(List<FileTransferEntity> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.delete_records);

        String deleteFile = getString(R.string.delete_local_file_sametime);
        CharSequence[] sequences = new CharSequence[1];
        sequences[0] = deleteFile;
        boolean[] booleans = new boolean[1];
        booleans[0] = true;
        builder.setMultiChoiceItems(sequences, booleans, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                booleans[which] = isChecked;
            }
        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doDeleteSelectedItem(list, booleans[0]);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void doDeleteSelectedItem(List<FileTransferEntity> list, boolean isDeleteLocalFile) {
        getViewModel().getShowLoadingDialogLiveData().setValue(true);

        BackgroundJobManagerImpl.getInstance().cancelDownloadWorker();

        getViewModel().removeSpecialDownloadListTask(list, isDeleteLocalFile, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {

                BackgroundJobManagerImpl.getInstance().startDownloadChainWorker();

                //You never know which item a user will select, so we need to remove them one by one, and then resort.
                for (FileTransferEntity fileTransferEntity : list) {
                    removeSpecialEntity(fileTransferEntity.uid);
                }

                getViewModel().getShowLoadingDialogLiveData().setValue(false);

                ToastUtils.showLong(R.string.deleted);
            }
        });
    }

    @Override
    public void restartSelectedItems(List<FileTransferEntity> list) {
        getViewModel().restartDownload(list, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                ToastUtils.showLong(R.string.transfer_list_restart_all);
                BackgroundJobManagerImpl.getInstance().startDownloadChainWorker();
            }
        });
    }

    /**
     * cancel all download tasks
     */
    public void cancelAllTasks() {
        BackgroundJobManagerImpl.getInstance().cancelDownloadWorker();

        getViewModel().cancelAllDownloadTask(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                ToastUtils.showLong(R.string.cancel_download);

                refreshData();
            }
        });
    }

    /**
     * remove all download tasks
     */
    public void removeAllTasks() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.delete_records);

        String deleteFile = getString(R.string.delete_local_file_sametime);
        CharSequence[] sequences = new CharSequence[1];
        sequences[0] = deleteFile;
        boolean[] booleans = new boolean[1];
        booleans[0] = true;
        builder.setMultiChoiceItems(sequences, booleans, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                booleans[which] = isChecked;
            }
        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //cancel worker
                BackgroundJobManagerImpl.getInstance().cancelDownloadWorker();

                getViewModel().removeAllDownloadTask(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        ToastUtils.showLong(R.string.deleted);

                        refreshData();
                    }
                }, booleans[0]);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();


    }

}

