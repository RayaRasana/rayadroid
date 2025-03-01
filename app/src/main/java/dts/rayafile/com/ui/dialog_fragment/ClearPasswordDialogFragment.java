package dts.rayafile.com.ui.dialog_fragment;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import dts.rayafile.com.R;
import dts.rayafile.com.ui.base.fragment.CustomDialogFragment;
import dts.rayafile.com.framework.datastore.DataManager;
import dts.rayafile.com.framework.data.DatabaseHelper;
import dts.rayafile.com.ui.base.fragment.RequestCustomDialogFragmentWithVM;
import dts.rayafile.com.ui.dialog_fragment.viewmodel.ClearCacheViewModel;
import dts.rayafile.com.ui.dialog_fragment.viewmodel.ClearPasswordViewModel;

import io.reactivex.functions.Consumer;

public class ClearPasswordDialogFragment extends RequestCustomDialogFragmentWithVM<ClearPasswordViewModel> {
    public static ClearPasswordDialogFragment newInstance() {
        Bundle args = new Bundle();

        ClearPasswordDialogFragment fragment = new ClearPasswordDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.view_dialog_message_textview;
    }

    @Override
    public int getDialogTitleRes() {
        return R.string.clear_password_title;
    }

    @Override
    protected void onPositiveClick() {
        getViewModel().clear(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                refreshData();
                dismiss();
            }
        });
    }

    @Override
    protected void initView(LinearLayout containerView) {
        super.initView(containerView);

        //set message
        TextView textView = containerView.findViewById(R.id.message_view);
        textView.setText(R.string.clear_password_warning);
    }

}
