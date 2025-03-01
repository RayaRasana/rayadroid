//package dts.rayafile.com.ui.file;
//
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import androidx.appcompat.widget.Toolbar;
//import androidx.lifecycle.Observer;
//import androidx.recyclerview.widget.LinearLayoutManager;
//
//import com.blankj.utilcode.util.FileUtils;
//import com.blankj.utilcode.util.ToastUtils;
//import com.google.android.material.dialog.MaterialAlertDialogBuilder;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Consumer;
//
//import dts.rayafile.com.R;
//import dts.rayafile.com.SeafException;
//import dts.rayafile.com.account.Account;
//import dts.rayafile.com.account.SupportAccountManager;
//import dts.rayafile.com.databinding.FileActivityBinding;
//import dts.rayafile.com.databinding.MultiFileActivityBinding;
//import dts.rayafile.com.framework.data.db.entities.DirentModel;
//import dts.rayafile.com.framework.data.db.entities.FileTransferEntity;
//import dts.rayafile.com.framework.data.db.entities.RepoModel;
//import dts.rayafile.com.framework.data.model.dirents.DirentFileModel;
//import dts.rayafile.com.framework.datastore.DataManager;
//import dts.rayafile.com.framework.util.SLogs;
//import dts.rayafile.com.framework.util.Utils;
//import dts.rayafile.com.framework.worker.ExistingFileStrategy;
//import dts.rayafile.com.ui.base.BaseActivityWithVM;
//import kotlin.Triple;
//
//public class MultiFileActivity extends BaseActivityWithVM<FileViewModel> {
//
//    private List<DirentModel> direntModels;
//    private ProgressBar progressBar;
//    private TextView progressText;
//    private int totalFiles;
//    private int downloadedFiles;
//    private File destinationFile;
//    private String repoId;
//    private MultiFileActivityBinding binding;
//    private MultiFileAdapter adapter;
//
//    public static Intent start(Context context, List<DirentModel> direntModels) {
//        Intent starter = new Intent(context, MultiFileActivity.class);
//        starter.putParcelableArrayListExtra("files", (ArrayList<DirentModel>) direntModels);
//        return starter;
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = MultiFileActivityBinding.inflate(getLayoutInflater());
//        direntModels = getIntent().getParcelableArrayListExtra("files");
//
//        // Initialize RecyclerView
//        adapter = new MultiFileAdapter(this, direntModels);
//        binding.filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//        binding.filesRecyclerView.setAdapter(adapter);
//
//        Intent intent = getIntent();
//        if (intent == null || !intent.hasExtra("files")) {
//            throw new IllegalArgumentException("missing files args");
//        }
//
//        direntModels = intent.getParcelableArrayListExtra("files");
//        totalFiles = direntModels.size();
//        downloadedFiles = 0;
//
//        initWidgets();
//        initViewModel();
//        startDownloadingFiles();
//    }
//
//    private void initWidgets() {
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setTitle("Downloading Files");
//
//        progressBar.setMax(100);
//    }
//
//    private void initViewModel() {
//        getViewModel().getProgressLiveData().observe(this, new Observer<Long[]>() {
//            @Override
//            public void onChanged(Long[] progressData) {
//                long transferredSize = progressData[0];
//                long totalSize = progressData[1];
//                onFileDownloadProgress(transferredSize, totalSize);
//            }
//        });
//
//        getViewModel().getOutFileLiveData().observe(this, new Observer<File>() {
//            @Override
//            public void onChanged(File downloadedFile) {
//                onFileDownloaded(downloadedFile);
//            }
//        });
//
//        getViewModel().getSeafExceptionLiveData().observe(this, new Observer<SeafException>() {
//            @Override
//            public void onChanged(SeafException seafException) {
//                onFileDownloadFailed(seafException);
//            }
//        });
//    }
//
//    private void startDownloadingFiles() {
//        // Loop through each file and start the download process
//        for (int i = 0; i < direntModels.size(); i++) {
//            DirentModel direntModel = direntModels.get(i);
//            repoId = direntModel.repo_id;
//            destinationFile = getLocalDestinationFile(repoId, direntModel.repo_name, direntModel.full_path);
//
//            // Start downloading each file in the list
//            downloadFile(direntModel, i);  // Pass index for updating progress in adapter
//        }
//    }
//
//    private void downloadFile(final DirentModel direntModel, final int position) {
//        getViewModel().loadFileDetail(repoId, direntModel.full_path, new Consumer<Triple<RepoModel, DirentFileModel, FileTransferEntity>>() {
//            @Override
//            public void accept(Triple<RepoModel, DirentFileModel, FileTransferEntity> triple) throws Exception {
//                RepoModel repoModel = triple.getFirst();
//                DirentFileModel direntFileModel = triple.getSecond();
//                FileTransferEntity fileTransferEntity = triple.getThird();
//
//                // Check existing file strategy (skip, replace, append, etc.)
//                ExistingFileStrategy strategy = checkFileStrategy(direntFileModel, fileTransferEntity);
//                if (ExistingFileStrategy.APPEND == strategy) {
//                    getViewModel().preDownload(repoModel, direntModel, destinationFile);
//                } else if (ExistingFileStrategy.SKIP == strategy) {
//                    // File is already present, mark it as downloaded
//                    onFileDownloaded(direntModel, destinationFile, position);
//                } else if (ExistingFileStrategy.ASK == strategy) {
//                    // Show dialog to ask the user
//                    showFileExistDialog(repoModel, direntModel, destinationFile, position);
//                } else if (ExistingFileStrategy.NOT_FOUND_IN_REMOTE == strategy) {
//                    // File not found remotely
//                    onFileDownloadFailed(SeafException.NOT_FOUND_EXCEPTION, position);
//                }
//            }
//        });
//    }
//    private void showFileExistDialog(RepoModel repoModel, DirentModel direntModel, File destinationFile) {
//        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
//        builder.setTitle("File Already Exists");
//        builder.setMessage("Do you want to overwrite or keep both?");
//
//        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
//
//        builder.setPositiveButton("Replace", (dialog, which) -> {
//            getViewModel().preDownload(repoModel, direntModel, destinationFile);
//        });
//
//        builder.setNegativeButton("Keep Both", (dialog, which) -> {
//            dialog.dismiss();
//            onFileDownloaded(destinationFile);
//        });
//
//        builder.show();
//    }
//
//    private ExistingFileStrategy checkFileStrategy(DirentFileModel direntFileModel, FileTransferEntity fileTransferEntity) {
//        if (null == direntFileModel || TextUtils.isEmpty(direntFileModel.id)) {
//            return ExistingFileStrategy.NOT_FOUND_IN_REMOTE;
//        }
//
//        if (!destinationFile.exists()) {
//            return ExistingFileStrategy.APPEND;
//        }
//
//        if (fileTransferEntity != null && fileTransferEntity.file_id.equals(direntFileModel.id)) {
//            return ExistingFileStrategy.SKIP;
//        }
//
//        long fileLastModified = FileUtils.getFileLastModified(destinationFile);
//        long direntLastModified = direntFileModel.getMtimeInMills();
//
//        if (direntLastModified > fileLastModified) {
//            return ExistingFileStrategy.ASK;
//        } else if (fileLastModified > direntLastModified) {
//            return ExistingFileStrategy.APPEND;
//        }
//
//        return ExistingFileStrategy.ASK;
//    }
//
//    private File getLocalDestinationFile(String repoId, String repoName, String fullPathInRepo) {
//        Account account = SupportAccountManager.getInstance().getCurrentAccount();
//        return DataManager.getLocalRepoFile(account, repoId, repoName, fullPathInRepo);
//    }
//
//    private void onFileDownloadProgress(long transferredSize, long totalSize, int position) {
//        // Calculate progress percentage
//        int progress = (int) (((double) transferredSize / (double) totalSize) * 100);
//
//        // Update the adapter with the progress for the specific file (position)
//        adapter.updateFileProgress(position, progress, transferredSize, totalSize);
//
//        String progressText = Utils.readableFileSize(transferredSize) + " / " + Utils.readableFileSize(totalSize);
//        adapter.updateFileProgressText(position, progressText);
//    }
//
//    private void onFileDownloaded(DirentModel direntModel, File destinationFile, int position) {
//        // Update the UI for the file at the given position in RecyclerView
//        adapter.updateFileStatus(position, "Downloaded", destinationFile.getAbsolutePath());
//
//        // Optionally notify the user
//        ToastUtils.showLong(String.format("File \"%s\" downloaded successfully!", direntModel.name));
//    }
//
//    private void onFileDownloadFailed(SeafException seafException, int position) {
//        // Update the UI to show failure for the file at the given position
//        adapter.updateFileStatus(position, "Failed", "");
//
//        // Notify the user
//        if (seafException == SeafException.NOT_FOUND_EXCEPTION) {
//            ToastUtils.showLong(String.format("The file \"%s\" has been deleted", direntModel.name));
//        } else {
//            ToastUtils.showLong(String.format("Failed to download file \"%s\"", seafException.getMessage()));
//        }
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            finish();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//}