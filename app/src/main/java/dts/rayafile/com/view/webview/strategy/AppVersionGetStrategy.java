package dts.rayafile.com.view.webview.strategy;

import com.blankj.utilcode.util.AppUtils;
import dts.rayafile.com.view.webview.IWebViewActionStrategy;

public class AppVersionGetStrategy implements IWebViewActionStrategy {
    @Override
    public String route(String paramsStr) {
        return AppUtils.getAppVersionName();
    }
}
