package dts.rayafile.com.ui.dialog_fragment;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.account.AccountUtils;
import dts.rayafile.com.ssl.CertsManager;
import dts.rayafile.com.ui.base.fragment.CustomDialogFragment;

public class SignOutDialogFragment extends CustomDialogFragment {
    public static SignOutDialogFragment newInstance() {

        Bundle args = new Bundle();

        SignOutDialogFragment fragment = new SignOutDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.view_dialog_message_textview;
    }

    @Override
    public int getDialogTitleRes() {
        return R.string.settings_account_sign_out_title;
    }

    @Override
    protected void onPositiveClick() {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();

        AccountUtils.logout(account);

        refreshData();
    }

    @Override
    protected void initView(LinearLayout containerView) {
        super.initView(containerView);

        //set message
        TextView textView = containerView.findViewById(R.id.message_view);
        textView.setText(R.string.settings_account_sign_out_confirm);
    }

}
