package dts.rayafile.com.ui.sdoc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Consumer;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Observer;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import dts.rayafile.com.R;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.databinding.ActivitySeaWebviewProBinding;
import dts.rayafile.com.databinding.ToolbarActionbarProgressBarBinding;
import dts.rayafile.com.enums.WebViewPreviewType;
import dts.rayafile.com.framework.data.model.sdoc.FileProfileConfigModel;
import dts.rayafile.com.framework.data.model.sdoc.FileRecordWrapperModel;
import dts.rayafile.com.framework.data.model.sdoc.OutlineItemModel;
import dts.rayafile.com.framework.data.model.sdoc.SDocPageOptionsModel;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.framework.util.StringUtils;
import dts.rayafile.com.listener.OnItemClickListener;
import dts.rayafile.com.ui.base.BaseActivityWithVM;
import dts.rayafile.com.ui.docs_comment.DocsCommentsActivity;
import dts.rayafile.com.ui.file_profile.FileProfileDialog;
import dts.rayafile.com.ui.sdoc.outline.SDocOutlineDialog;
import dts.rayafile.com.view.webview.OnWebPageListener;
import dts.rayafile.com.view.webview.PreloadWebView;
import dts.rayafile.com.view.webview.SeaWebView;

public class SDocWebViewActivity extends BaseActivityWithVM<SDocViewModel> {
    private ActivitySeaWebviewProBinding binding;
    private ToolbarActionbarProgressBarBinding toolBinding;

    private SeaWebView mWebView;
    private String repoId;
    private String path;
    private String targetUrl;

    private FileProfileConfigModel configModel;
    private SDocPageOptionsModel pageOptionsData;

    /**
     * not support, please use SeaWebViewActivity instead
     */
    public static void openSdoc(Context context, String repoName, String repoID, String path) {
        Intent intent = new Intent(context, SDocWebViewActivity.class);
        intent.putExtra("previewType", WebViewPreviewType.SDOC.name());
        intent.putExtra("repoName", repoName);
        intent.putExtra("repoID", repoID);
        intent.putExtra("filePath", path);
        ActivityUtils.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySeaWebviewProBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolBinding = ToolbarActionbarProgressBarBinding.bind(binding.toolProgressBar.getRoot());


        initUI();

        init();

        initViewModel();

        mWebView.setOnWebPageListener(new OnWebPageListener() {
            @Override
            public void onPageFinished(WebView view, String url) {
                canLoadPageConfigData();
            }
        });

        //let's go
        mWebView.load(targetUrl);
    }

    private void init() {
        Intent intent = getIntent();

        if (!intent.hasExtra("previewType")) {
            throw new IllegalArgumentException("need a previewType param");
        }

        String previewType = intent.getStringExtra("previewType");
        if (!WebViewPreviewType.contains(previewType)) {
            throw new IllegalArgumentException("need a previewType param");
        }

        WebViewPreviewType previewTypeEnum = WebViewPreviewType.valueOf(previewType);

        if (previewTypeEnum == WebViewPreviewType.SDOC) {

            String repoName = intent.getStringExtra("repoName");
            repoId = intent.getStringExtra("repoID");
            path = intent.getStringExtra("filePath");

            if (TextUtils.isEmpty(repoId) || TextUtils.isEmpty(path)) {
                throw new IllegalArgumentException("repoId or path is null");
            }

            Account account = SupportAccountManager.getInstance().getCurrentAccount();
            if (account != null) {
                targetUrl = account.server + "lib/" + repoId + "/file" + path;
            } else {
                throw new IllegalArgumentException("no login");
            }
        } else {
            throw new IllegalArgumentException("previewType is not SDOC");
        }
    }

    private void initUI() {
        Toolbar toolbar = toolBinding.toolbarActionbar;
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

        mWebView = PreloadWebView.getInstance().getWebView(this);


        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(mWebView.getSettings(), true);
        }

        //chrome client
        mWebView.setWebChromeClient(mWebChromeClient);

        NestedScrollView.LayoutParams ll = new NestedScrollView.LayoutParams(-1, -1);
        mWebView.setLayoutParams(ll);
        binding.nsv.addView(mWebView);

        binding.sdocOutline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOutlineDialog();
            }
        });
        binding.sdocProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfileDialog();
            }
        });
        binding.sdocComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommentsActivity();
            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mWebView != null && mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void initViewModel() {
        getViewModel().getFileDetailLiveData().observe(this, new Observer<FileProfileConfigModel>() {
            @Override
            public void onChanged(FileProfileConfigModel fileProfileConfigModel) {
                configModel = fileProfileConfigModel;
                hideProgressBar();
            }
        });

        getViewModel().getSdocRecordLiveData().observe(this, new Observer<FileRecordWrapperModel>() {
            @Override
            public void onChanged(FileRecordWrapperModel fileRecordWrapperModel) {
                FileProfileDialog dialog = FileProfileDialog.newInstance(configModel.detail, fileRecordWrapperModel, configModel.users.user_list, false);
                dialog.show(getSupportFragmentManager(), FileProfileDialog.class.getSimpleName());
            }
        });
    }

    private void showOutlineDialog() {
        readSDocOutlineList(new Consumer<String>() {
            @Override
            public void accept(String s) {
                SDocOutlineDialog dialog = SDocOutlineDialog.newInstance(s);
                dialog.setOnItemClickListener(new OnItemClickListener<OutlineItemModel>() {
                    @Override
                    public void onItemClick(OutlineItemModel outlineItemModel, int position) {

                        callJsOutline(outlineItemModel);
                    }
                });
                dialog.show(getSupportFragmentManager(), SDocOutlineDialog.class.getSimpleName());
            }
        });
    }

    private void callJsOutline(OutlineItemModel outlineItemModel) {
        String param = GsonUtils.toJson(outlineItemModel);
        mWebView.callJsFunction("sdoc.outline.data.select", param, new CallBackFunction() {
            @Override
            public void onCallBack(String data) {
                SLogs.e(data);
            }
        });
    }

    private void showProfileDialog() {
        if (configModel == null) {
            return;
        }

        readSDocPageOptionsData(new Consumer<SDocPageOptionsModel>() {
            @Override
            public void accept(SDocPageOptionsModel model) {
                if (model.enableMetadataManagement) {
                    if (configModel != null && configModel.metadataConfigModel != null && configModel.metadataConfigModel.enabled) {
                        getViewModel().loadRecords(repoId, path);
                    } else if (configModel != null) {
                        FileProfileDialog dialog = FileProfileDialog.newInstance(configModel.detail, configModel.users.user_list);
                        dialog.show(getSupportFragmentManager(), FileProfileDialog.class.getSimpleName());
                    }
                } else {
                    FileProfileDialog dialog = FileProfileDialog.newInstance(configModel.detail, configModel.users.user_list);
                    dialog.show(getSupportFragmentManager(), FileProfileDialog.class.getSimpleName());
                }
            }
        });
    }

    private void showCommentsActivity() {
        readSDocPageOptionsData(new Consumer<SDocPageOptionsModel>() {
            @Override
            public void accept(SDocPageOptionsModel model) {
                DocsCommentsActivity.start(SDocWebViewActivity.this, model);
            }
        });
    }

    private void readSDocPageOptionsData(Consumer<SDocPageOptionsModel> continuation) {
        if (pageOptionsData != null) {
            continuation.accept(pageOptionsData);
            return;
        }
        String js =
                "(function() {" +
                        "   if (window.app && window.app.pageOptions) {" +
                        "       return JSON.stringify(window.app.pageOptions);" +
                        "   } else {" +
                        "       return null;" +
                        "   }" +
                        "})();";
        mWebView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!TextUtils.isEmpty(value)) {
                    value = StringUtils.deString(value).replace("\\", "");
                    pageOptionsData = GsonUtils.fromJson(value, SDocPageOptionsModel.class);
                    if (pageOptionsData != null) {
                        continuation.accept(pageOptionsData);
                    } else {
                        SLogs.e("read sodc page options data from web, an exception occurred in the parsing data");
                        SLogs.e(value);
                        ToastUtils.showShort(R.string.unknow_error);
                    }

                } else {
                    SLogs.e("read sodc page options data from web: " + value);
                    ToastUtils.showShort(R.string.unknow_error);
                }
            }
        });
    }

    private void readSDocOutlineList(Consumer<String> continuation) {
        String js =
                "(function() {" +
                        "   if (window.seadroid && window.seadroid.outlines) {" +
                        "       return JSON.stringify(window.seadroid.outlines);" +
                        "   } else {" +
                        "       return null;" +
                        "   }" +
                        "})();";
        mWebView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (TextUtils.isEmpty(value)) {
                    SLogs.e(value);
                    ToastUtils.showShort(R.string.empty_data);
                    continuation.accept(value);
                    return;
                }

                value = StringUtils.deStringReturnNonNull(value).replace("\\", "");
                if (continuation != null) {
                    continuation.accept(value);
                }
            }
        });
    }

    @Deprecated
    private void readSeafileTokenData(Consumer<String> continuation) {
        String js =
                "(function() {" +
                        "   if (window.seafile && window.seafile.accessToken) {" +
                        "       return JSON.stringify(window.seafile.accessToken);" +
                        "   } else {" +
                        "       return null;" +
                        "   }" +
                        "})();";
        mWebView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                SLogs.e(value);
                if (TextUtils.isEmpty(value)) {
                    SLogs.d("doc uuid is empty.");
                    ToastUtils.showShort("outline is empty.");
                    return;
                }

                value = StringUtils.deStringReturnNonNull(value).replace("\\", "");
                value = StringUtils.deStringReturnNonNull(value);

                if (continuation != null) {
                    continuation.accept(value);
                } else {

                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    private int curProgress = 0;
    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            curProgress = newProgress;
            setBarProgress(curProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
//            toolBinding.toolbarActionbar.setTitle(title);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage != null) {
                switch (consoleMessage.messageLevel()) {
                    case ERROR:
                        SLogs.e("web e log: line: " + consoleMessage.lineNumber() + ", message: " + consoleMessage.message());
                        break;
                    case DEBUG:
                        SLogs.d("web d log: line: " + consoleMessage.lineNumber() + ", message: " + consoleMessage.message());
                        break;
                    case WARNING:
                        SLogs.w("web w log: line: " + consoleMessage.lineNumber() + ", message: " + consoleMessage.message());
                        break;
                    case TIP:
                        SLogs.i("web i log: line: " + consoleMessage.lineNumber() + ", message: " + consoleMessage.message());
                        break;
                    default:
                        SLogs.e("web default log: line: " + consoleMessage.lineNumber() + ", message: " + consoleMessage.message());
                        break;
                }
            }
            return super.onConsoleMessage(consoleMessage);
        }
    };

    private void setBarProgress(int p) {
        toolBinding.toolProgressBar.setProgress(p, true);

        if (p != 100) {
            if (toolBinding.toolProgressBar.getVisibility() != View.VISIBLE) {
                toolBinding.toolProgressBar.setVisibility(View.VISIBLE);
            }
        }

        hideProgressBar();
    }

    private void hideProgressBar() {
        if (configModel != null && curProgress == 100) {
            toolBinding.toolProgressBar.setVisibility(View.GONE);
        }
    }

    private void canLoadPageConfigData() {
        readSDocPageOptionsData(new Consumer<SDocPageOptionsModel>() {
            @Override
            public void accept(SDocPageOptionsModel model) {
                getViewModel().loadFileDetail(repoId, path, model.enableMetadataManagement);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        destroyWebView();
    }

    public void destroyWebView() {
        if (mWebView == null) {
            return;
        }

        binding.nsv.removeView(mWebView);

        mWebView.loadUrl("about:blank");
        mWebView.stopLoading();
        mWebView.destroy();
    }
}
