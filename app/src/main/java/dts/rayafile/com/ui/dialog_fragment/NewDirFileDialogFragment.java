package dts.rayafile.com.ui.dialog_fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.blankj.utilcode.util.ToastUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.ui.base.fragment.RequestCustomDialogFragmentWithVM;
import dts.rayafile.com.framework.data.model.ResultModel;
import dts.rayafile.com.framework.data.model.dirents.FileCreateModel;
import dts.rayafile.com.ui.dialog_fragment.viewmodel.NewDirViewModel;

public class NewDirFileDialogFragment extends RequestCustomDialogFragmentWithVM<NewDirViewModel> {
    private String parentDir, repoId;
    private boolean isDir;

    public static NewDirFileDialogFragment newInstance(String repoId, String parentDir, boolean isDir) {
        Bundle args = new Bundle();
        args.putString("repo_id", repoId);
        args.putString("parent_dir", parentDir);
        args.putBoolean("is_dir", isDir);
        NewDirFileDialogFragment fragment = new NewDirFileDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        repoId = args.getString("repo_id");
        parentDir = args.getString("parent_dir");
        isDir = args.getBoolean("is_dir");

    }

    @Override
    protected int getLayoutId() {
        return R.layout.view_dialog_new_file;
    }

    @Override
    public int getDialogTitleRes() {
        return isDir ? R.string.create_new_dir : R.string.create_new_file;
    }

    @Override
    protected void onPositiveClick() {
        if (!checkData()) {
            return;
        }

        EditText name = getDialogView().findViewById(R.id.new_file_name);
        String pathName = name.getText().toString();
        pathName = parentDir + "/" + pathName;
        if (isDir) {
            getViewModel().createNewDir(pathName, repoId);
        } else {
            getViewModel().createNewFile(pathName, repoId);
        }
    }

    @Override
    protected void initView(LinearLayout containerView) {
        super.initView(containerView);

        if (TextUtils.isEmpty(parentDir)) {
            throw new IllegalArgumentException("this dialogFragment need parentDir param");
        }

        if (TextUtils.isEmpty(repoId)) {
            throw new IllegalArgumentException("this dialogFragment need repoId param");
        }
    }

    @Override
    protected void initViewModel() {
        super.initViewModel();

        if (isDir) {
            getViewModel().getCreateDirLiveData().observe(this, new Observer<ResultModel>() {
                @Override
                public void onChanged(ResultModel resultModel) {
                    if (!TextUtils.isEmpty(resultModel.error_msg)) {
                        setInputError(R.id.text_input, resultModel.error_msg);
                    } else {

                        EditText name = getDialogView().findViewById(R.id.new_file_name);
                        String pathName = name.getText().toString();

                        ToastUtils.showLong(getString(R.string.create_new_folder_success, pathName));

                        refreshData();

                        dismiss();
                    }
                }
            });
        } else {
            getViewModel().getCreateFileLiveData().observe(this, new Observer<FileCreateModel>() {
                @Override
                public void onChanged(FileCreateModel fileCreateModel) {
                    if (!TextUtils.isEmpty(fileCreateModel.error_msg)) {
                        setInputError(R.id.text_input, fileCreateModel.error_msg);
                    } else {
                        EditText name = getDialogView().findViewById(R.id.new_file_name);
                        String pathName = name.getText().toString();

                        ToastUtils.showLong(getString(R.string.create_new_file_success, pathName));

                        refreshData();

                        dismiss();
                    }
                }
            });
        }


        getViewModel().getRefreshLiveData().observe(this, aBoolean -> showLoading(aBoolean));
    }

    private boolean checkData() {
        EditText editText = getDialogView().findViewById(R.id.new_file_name);
        Editable editable = editText.getText();
        if (editable == null || editable.length() == 0) {
            if (isDir) {
                ToastUtils.showLong(R.string.dir_name_empty);
            } else {
                ToastUtils.showLong(R.string.file_name_empty);
            }
            return false;
        }
        return true;
    }
}
