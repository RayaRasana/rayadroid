package dts.rayafile.com.ui.base.adapter;

import com.chad.library.adapter4.BaseMultiItemAdapter;
import dts.rayafile.com.framework.util.SLogs;

public abstract class BaseMultiAdapter<M> extends BaseMultiItemAdapter<M> {
    public void d(String d) {
        SLogs.d(this.getClass().getSimpleName() + " => " + d);
    }
}
