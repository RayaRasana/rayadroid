package dts.rayafile.com.view.webview.strategy;

import android.app.Activity;
import android.content.Context;
import android.content.MutableContextWrapper;

import androidx.appcompat.app.AppCompatActivity;

import dts.rayafile.com.annotation.Todo;
import dts.rayafile.com.annotation.Unstable;
import dts.rayafile.com.view.webview.IWebViewActionStrategy;


@Unstable
@Todo
public class PageStatusColorSetStrategy implements IWebViewActionStrategy {
    private Context context;

    public PageStatusColorSetStrategy(Context context) {
        this.context = context;
    }

    @Override
    public String route(String paramsStr) {
        if (context != null) {
            MutableContextWrapper c = (MutableContextWrapper) context;
            if (c.getBaseContext() instanceof Activity) {
                AppCompatActivity a = (AppCompatActivity) c.getBaseContext();
                //todo
            } else {
                throw new IllegalArgumentException("Context is not activity");
            }
        }

        return null;
    }
}
