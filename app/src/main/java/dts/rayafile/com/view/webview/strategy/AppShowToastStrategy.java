package dts.rayafile.com.view.webview.strategy;

import com.blankj.utilcode.util.ToastUtils;
import dts.rayafile.com.view.webview.IWebViewActionStrategy;


public class AppShowToastStrategy implements IWebViewActionStrategy {
    @Override
    public String route(String paramsStr) {
        ToastUtils.showLong(paramsStr);
        return null;
    }
}
