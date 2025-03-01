package dts.rayafile.com.listener;

import dts.rayafile.com.ui.selector.folder_selector.FileBean;

public interface OnFileItemChangeListener {
    void onChanged(FileBean fileBean, int position, boolean isChecked);
}
