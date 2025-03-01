package dts.rayafile.com.ui.dialog_fragment;

import android.os.Bundle;
import android.os.Parcelable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.blankj.utilcode.util.CollectionUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.ui.base.fragment.RequestCustomDialogFragmentWithVM;
import dts.rayafile.com.ui.dialog_fragment.viewmodel.DeleteDirsViewModel;

import java.util.ArrayList;
import java.util.List;

public class DeleteFileDialogFragment extends RequestCustomDialogFragmentWithVM<DeleteDirsViewModel> {
    private List<String> dirents;
    private boolean isDir = false;

    public static DeleteFileDialogFragment newInstance(List<String> direntIds) {
        DeleteFileDialogFragment fragment = new DeleteFileDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("dirent_ids", new ArrayList<>(direntIds));
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        if (bundle == null || !bundle.containsKey("dirent_ids")) {
            throw new RuntimeException("need a dirent_ids param");
        }

        dirents = bundle.getStringArrayList("dirent_ids");

    }

    @Override
    protected int getLayoutId() {
        return R.layout.view_dialog_delete_dirent;
    }

    @Override
    protected void onPositiveClick() {

        if (CollectionUtils.isEmpty(dirents)) {
            return;
        }

        getViewModel().delete(dirents, false);
    }

    @Override
    protected void initViewModel() {
        super.initViewModel();

        getViewModel().getRefreshLiveData().observe(this, this::showLoading);

        getViewModel().getActionLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                refreshData();

                dismiss();
            }
        });
    }

    @Override
    public int getDialogTitleRes() {
        return isDir ? R.string.delete_dir : R.string.delete_file_f;
    }

    @Override
    protected void initView(LinearLayout containerView) {
        super.initView(containerView);

        //set message
        TextView textView = containerView.findViewById(R.id.message_view);
        textView.setText(R.string.delete_file);


    }


}
