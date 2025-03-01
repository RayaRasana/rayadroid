package dts.rayafile.com.ui.dialog_fragment;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.framework.datastore.StorageManager;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.worker.BackgroundJobManagerImpl;
import dts.rayafile.com.ui.base.fragment.CustomDialogFragment;
import dts.rayafile.com.ui.base.fragment.RequestCustomDialogFragmentWithVM;
import dts.rayafile.com.ui.camera_upload.CameraUploadManager;
import dts.rayafile.com.ui.dialog_fragment.viewmodel.SwitchStorageViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

public class SwitchStorageDialogFragment extends RequestCustomDialogFragmentWithVM<SwitchStorageViewModel> {

    private List<RadioButton> buttonList = new ArrayList<>();
    private int currentLocationId = -1;
    private RadioGroup group;

    public static SwitchStorageDialogFragment newInstance() {

        Bundle args = new Bundle();

        SwitchStorageDialogFragment fragment = new SwitchStorageDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.view_dialog_switch_storage;
    }

    @Override
    protected void onPositiveClick() {
        StorageManager.Location location = null;
        int selectedId = group.getCheckedRadioButtonId();
        for (RadioButton b : buttonList) {
            if (b.getId() == selectedId) {
                location = (StorageManager.Location) b.getTag();
                break;
            }
        }

        getViewModel().switchStorage(location, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                dismiss();
            }
        });
    }

    @Override
    public int getDialogTitleRes() {
        return R.string.settings_cache_location_title;
    }

    @Override
    protected void initView(LinearLayout containerView) {
        super.initView(containerView);

        group = getDialogView().findViewById(R.id.storage_options);
        ArrayList<StorageManager.Location> options = StorageManager.getInstance().getStorageLocations();

        for (StorageManager.Location location : options) {
            RadioButton b = new RadioButton(getContext());
            b.setText(location.description);
            b.setTag(location);
            b.setEnabled(location.available);
            group.addView(b);
            buttonList.add(b);

            if (location.currentSelection)
                currentLocationId = b.getId();

        }
        group.check(currentLocationId);

    }
}
