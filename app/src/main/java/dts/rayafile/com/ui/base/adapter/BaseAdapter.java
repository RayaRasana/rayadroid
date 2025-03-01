package dts.rayafile.com.ui.base.adapter;

import com.chad.library.adapter4.BaseQuickAdapter;
import dts.rayafile.com.ui.base.viewholder.BaseViewHolder;
import dts.rayafile.com.framework.util.SLogs;

public abstract class BaseAdapter<M, VH extends BaseViewHolder> extends BaseQuickAdapter<M, VH> {
    public void d(String d) {
        SLogs.d(this.getClass().getSimpleName() + " => " + d);
    }
}
