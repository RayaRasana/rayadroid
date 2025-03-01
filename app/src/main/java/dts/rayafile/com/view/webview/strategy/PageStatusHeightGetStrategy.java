package dts.rayafile.com.view.webview.strategy;

import com.blankj.utilcode.util.BarUtils;
import dts.rayafile.com.view.webview.IWebViewActionStrategy;

public class PageStatusHeightGetStrategy implements IWebViewActionStrategy {
    @Override
    public String route(String paramsStr) {
        //page.status.height.get
        return String.valueOf(BarUtils.getStatusBarHeight());
    }
}
