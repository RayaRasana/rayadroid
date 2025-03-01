package dts.rayafile.com.ui.base.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.chad.library.adapter4.QuickAdapterHelper;
import dts.rayafile.com.ui.base.adapter.BaseAdapter;
import dts.rayafile.com.ui.base.adapter.BaseMultiAdapter;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.framework.util.TUtil;

public class BaseFragmentWithVM<VM extends BaseViewModel> extends BaseFragment {
    private VM tvm;

    public VM getViewModel() {
        return tvm;
    }

    private QuickAdapterHelper helper;

    public QuickAdapterHelper createAdapterHelper(BaseAdapter<?, ?> adapter) {
        if (null == helper) {
            helper = new QuickAdapterHelper.Builder(adapter).build();
        }
        return helper;
    }

    public QuickAdapterHelper createMuiltAdapterHelper(BaseMultiAdapter<?> adapter) {
        if (null == helper) {
            helper = new QuickAdapterHelper.Builder(adapter).build();
        }
        return helper;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViewModelClass();
    }

    protected void initViewModelClass() {
        VM t = TUtil.getT(this, 0);
        if (t == null) {
            throw new IllegalStateException("VM generic parameters that inherit BaseViewModel cannot be instantiated");
        }

        ViewModel viewModel = new ViewModelProvider(this).get(t.getClass());
        tvm = (VM) viewModel;
    }
}
