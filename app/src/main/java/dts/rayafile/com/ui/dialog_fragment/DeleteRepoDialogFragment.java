package dts.rayafile.com.ui.dialog_fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.ToastUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.ui.base.fragment.RequestCustomDialogFragmentWithVM;
import dts.rayafile.com.framework.data.model.ResultModel;
import dts.rayafile.com.ui.dialog_fragment.viewmodel.DeleteRepoViewModel;

import java.util.ArrayList;
import java.util.List;

public class DeleteRepoDialogFragment extends RequestCustomDialogFragmentWithVM<DeleteRepoViewModel> {
    private List<String> repoIds;

    public static DeleteRepoDialogFragment newInstance(List<String> repoIds) {
        Bundle args = new Bundle();
        args.putStringArrayList("repo_ids", new ArrayList<>(repoIds));
        DeleteRepoDialogFragment fragment = new DeleteRepoDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null || !getArguments().containsKey("repo_ids")) {
            throw new IllegalArgumentException("this dialogFragment need Arguments");
        }

        repoIds = getArguments().getStringArrayList("repo_ids");
        if (CollectionUtils.isEmpty(repoIds)) {
            throw new IllegalArgumentException("need repoIds param");
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.view_dialog_message_textview;
    }

    @Override
    public int getDialogTitleRes() {
        return R.string.delete_repo_title;
    }

    @Override
    protected void onPositiveClick() {
        getViewModel().deleteRepo(repoIds);
    }

    @Override
    protected void initViewModel() {
        super.initViewModel();

        getViewModel().getActionLiveData().observe(this, new Observer<ResultModel>() {
            @Override
            public void onChanged(ResultModel resultModel) {

                if (!TextUtils.isEmpty(resultModel.error_msg)) {
                    ToastUtils.showLong(resultModel.error_msg);
                    dismiss();
                    return;
                }

                refreshData();

                dismiss();
            }
        });

        getViewModel().getRefreshLiveData().observe(this, this::showLoading);
    }

    @Override
    protected void initView(LinearLayout containerView) {
        super.initView(containerView);

        //set message
        TextView textView = containerView.findViewById(R.id.message_view);
        textView.setText(R.string.delete_repo);
    }
}
