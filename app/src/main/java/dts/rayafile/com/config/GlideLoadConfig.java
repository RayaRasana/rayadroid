package dts.rayafile.com.config;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.ui.WidgetUtils;

public class GlideLoadConfig {

    public static GlideUrl getGlideUrl(String url) {

        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            return new GlideUrl(url, new LazyHeaders.Builder().build());
        }

        String token = account.token;

        return new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", "Token " + token)
                .build());
    }
//
//    public static GlideUrl getGlideUrl(String url, String token) {
//        return new GlideUrl(url, new LazyHeaders.Builder()
//                .addHeader("Authorization", "Token " + token)
//                .build());
//    }

    public static RequestOptions getAvatarOptions() {
        return new RequestOptions()
                .fallback(R.drawable.default_avatar)
                .placeholder(R.drawable.default_avatar)
                .override(WidgetUtils.getThumbnailWidth(), WidgetUtils.getThumbnailWidth());
    }

    private final static RequestOptions _cacheableThumbnailOptions = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .error(R.drawable.icon_image_error_filled)
            .override(128);

    /**
     * Get cacheable thumbnail options, width and height are both 128
     */
    public static RequestOptions getCacheableThumbnailOptions() {
        return _cacheableThumbnailOptions;
    }

    public static RequestOptions getOptions() {
        return new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fallback(R.drawable.file_image)
                .placeholder(R.drawable.file_image);
    }


    public static RequestOptions getCustomDrawableOptions(@DrawableRes int resId) {
        return new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fallback(resId)
                .placeholder(resId);
    }

    public static RequestOptions getOptions(String key) {
        return new RequestOptions()
                .fallback(R.drawable.file_image)
                .placeholder(R.drawable.file_image)
                .signature(new ObjectKey(key))
                .override(WidgetUtils.getThumbnailWidth(), WidgetUtils.getThumbnailWidth());
    }
}
