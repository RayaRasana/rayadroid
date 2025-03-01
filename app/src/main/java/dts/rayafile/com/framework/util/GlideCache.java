package dts.rayafile.com.framework.util;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.http.UnsafeOkHttpClient;
import dts.rayafile.com.framework.http.interceptor.CurrentTokenInterceptor;
import dts.rayafile.com.framework.http.interceptor.HeaderInterceptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@GlideModule
public class GlideCache extends AppGlideModule {


    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        super.applyOptions(context, builder);
//        String rootPath = SeadroidApplication.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        File[] externalMediaDirs = SeadroidApplication.getAppContext().getExternalMediaDirs();
        String rootPath = externalMediaDirs[0].getAbsolutePath();
        File dirPath = new File(rootPath + "/GlideCache/");
        builder.setDiskCache(new DiskLruCacheFactory(dirPath.getAbsolutePath(), 1024 * 1024 * 500));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        try {
            OkHttpClient client = getClient();
            registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
        } catch (IllegalStateException e) {
            SLogs.d("No current account?");
        }
    }

    private OkHttpClient getClient() {
        UnsafeOkHttpClient unsafeOkHttpClient = new UnsafeOkHttpClient();
        OkHttpClient.Builder builder = unsafeOkHttpClient.getBuilder();
        builder.followRedirects(true);
        builder.addInterceptor(new CurrentTokenInterceptor());
//        builder.addInterceptor(new Interceptor() {
//            @Override
//            public Response intercept(Chain chain) throws IOException {
//                Request request = chain.request();
//                String url = request.url().toString();
//
//                String kie = CookieManager.getInstance().getCookie(URLs.getHost(url));
//                Request.Builder requestBuilder = request.newBuilder();
//                if (kie != null) {
//                    requestBuilder.addHeader("Cookie", kie);
//                }
//
//                Request newRequest = requestBuilder.build();
//                return chain.proceed(newRequest);
//            }
//        });
        return builder.build();
    }
}
