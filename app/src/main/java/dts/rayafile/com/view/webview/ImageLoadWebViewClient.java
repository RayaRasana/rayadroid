package dts.rayafile.com.view.webview;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.util.URLs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ImageLoadWebViewClient extends WebViewClient {
    private final String TOKEN = HttpIO.getCurrentInstance().getAccount().getToken();
    private final String SERVER_URL = HttpIO.getCurrentInstance().getServerUrl();


    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        String format = URLs.getFileFormat(url);
        if (TextUtils.isEmpty(format)) {
            return super.shouldInterceptRequest(view, request);
        }

        if (!url.startsWith(SERVER_URL)) {
            return super.shouldInterceptRequest(view, request);
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format);
        if (!TextUtils.isEmpty(mimeType) && mimeType.startsWith("image/")) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Token " + TOKEN);

            InputStream stream = new ByteArrayInputStream(new byte[0]);
            return new WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, stream);
        }

        return super.shouldInterceptRequest(view, request);
    }
}
