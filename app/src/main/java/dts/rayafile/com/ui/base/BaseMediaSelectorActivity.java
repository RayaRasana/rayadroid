package dts.rayafile.com.ui.base;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.blankj.utilcode.util.ToastUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.util.TakeCameras;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.ui.bottomsheetmenu.BottomSheetHelper;
import dts.rayafile.com.ui.bottomsheetmenu.OnMenuClickListener;

import java.io.File;
import java.util.List;

import kotlin.Pair;

public class BaseMediaSelectorActivity<T extends BaseViewModel> extends BaseActivityWithVM<T> {
    private ActivityResultLauncher<String> cameraPermissionLauncher, cameraPermissionLauncher1, storagePermissionLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<Uri> shootVideoLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMediaLauncher;
    private ActivityResultLauncher<String> fileChooseLauncher;
    private Pair<Uri, File> uriPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initLauncher();
    }

    private void initLauncher() {
        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    uriPair = TakeCameras.buildTakePhotoUri(BaseMediaSelectorActivity.this);
                    takePhotoLauncher.launch(uriPair.getFirst());
                } else {
                    ToastUtils.showLong(R.string.permission_camera);
                }
            }
        });

        cameraPermissionLauncher1 = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    uriPair = TakeCameras.buildTakePhotoUri(BaseMediaSelectorActivity.this);
                    shootVideoLauncher.launch(uriPair.getFirst());
                } else {
                    ToastUtils.showLong(R.string.permission_camera);
                }
            }
        });

        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean o) {
                if (o) {
                    fileChooseLauncher.launch("*/*");
                } else {
                    ToastUtils.showLong(R.string.permission_manage_external_storage_rationale);
                }
            }
        });

        takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean o) {
                if (!o) {
                    return;
                }
                onMediaPicked(uriPair.getFirst());
            }
        });

        shootVideoLauncher = registerForActivityResult(new ActivityResultContracts.CaptureVideo(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean o) {
                if (!o) {
                    return;
                }
                onMediaPicked(uriPair.getFirst());
            }
        });

        pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri o) {
                onMediaPicked(o);
            }
        });

        pickMultipleMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(9), new ActivityResultCallback<List<Uri>>() {
            @Override
            public void onActivityResult(List<Uri> o) {
                for (Uri uri : o) {
                    onMediaPicked(uri);
                }
            }
        });

        fileChooseLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri o) {
                onMediaPicked(o);
            }
        });
    }

    public void onMediaPicked(Uri uri) {
        SLogs.d(uri.toString());
    }

    ///////////////////////////////////////////////////////

    public void showPickPhotoSheetDialog(boolean isPickMultiWhenMenuIdIsViewFile) {
        BottomSheetHelper.buildSheet(this, R.menu.bottom_sheet_camera_album_select, new OnMenuClickListener() {
            @Override
            public void onMenuClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.take_photo) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                } else if (menuItem.getItemId() == R.id.view_file) {
                    if (isPickMultiWhenMenuIdIsViewFile) {
                        pickMultipleMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
                    } else {
                        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
                    }
                }
            }
        }).show(getSupportFragmentManager());
    }

    public void showPickPhotoAndVideoSheetDialog(boolean isPickMultiWhenMenuIdIsViewFile) {
        BottomSheetHelper.buildSheet(this, R.menu.bottom_sheet_camera_album_select, new OnMenuClickListener() {
            @Override
            public void onMenuClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.take_photo) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                } else if (menuItem.getItemId() == R.id.view_file) {
                    if (isPickMultiWhenMenuIdIsViewFile) {
                        pickMultipleMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                                .build());
                    } else {
                        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                                .build());
                    }
                }
            }
        }).show(getSupportFragmentManager());
    }

    public void pickFile() {
        storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

}
