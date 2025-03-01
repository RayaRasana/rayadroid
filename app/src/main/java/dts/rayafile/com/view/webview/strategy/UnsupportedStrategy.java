package dts.rayafile.com.view.webview.strategy;


import com.blankj.utilcode.util.ToastUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.view.webview.IWebViewActionStrategy;


public class UnsupportedStrategy implements IWebViewActionStrategy {
    @Override
    public String route(String paramsStr) {
        ToastUtils.showLong(R.string.not_supported);
        return null;
    }
}
