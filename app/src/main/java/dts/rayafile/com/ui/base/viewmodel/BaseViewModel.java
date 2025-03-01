package dts.rayafile.com.ui.base.viewmodel;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ToastUtils;
import dts.rayafile.com.BuildConfig;
import dts.rayafile.com.R;
import dts.rayafile.com.SeadroidApplication;
import dts.rayafile.com.SeafException;
import dts.rayafile.com.annotation.Todo;
import dts.rayafile.com.framework.data.model.ResultModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.framework.util.ExceptionUtils;
import dts.rayafile.com.framework.util.SLogs;
import dts.rayafile.com.ui.account.AccountService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import kotlin.Pair;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.HttpException;
import retrofit2.Response;

public class BaseViewModel extends ViewModel {
    private final MutableLiveData<Boolean> RefreshLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Pair<Integer, SeafException>> ExceptionLiveData = new MutableLiveData<>();
    private final MutableLiveData<SeafException> SeafExceptionLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ShowLoadingDialogLiveData = new MutableLiveData<>(false);

    public MutableLiveData<Boolean> getShowLoadingDialogLiveData() {
        return ShowLoadingDialogLiveData;
    }

    public MutableLiveData<Boolean> getRefreshLiveData() {
        return RefreshLiveData;
    }

    public MutableLiveData<Pair<Integer, SeafException>> getExceptionLiveData() {
        return ExceptionLiveData;
    }

    public MutableLiveData<SeafException> getSeafExceptionLiveData() {
        return SeafExceptionLiveData;
    }

    public void showRefresh() {
        getRefreshLiveData().setValue(true);
    }

    public void closeRefresh() {
        getRefreshLiveData().setValue(false);
    }

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public void completeRemoteWipe() {
        Single<Object> single = HttpIO.getCurrentInstance().execute(AccountService.class).deviceWiped();
        addSingleDisposable(single, new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                SLogs.d("device wiped");
            }
        });
    }

    private final Consumer<Throwable> throwable = throwable -> {
        SLogs.e(throwable);
        closeRefresh();

        if (BuildConfig.DEBUG) {
            ToastUtils.showLong(throwable.getMessage());
        }

        //check and callback
        checkException(throwable);
    };

    public void disposeAll() {
        compositeDisposable.clear();
        SLogs.d("CompositeDisposable clear all");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        SLogs.d("onCleared");

        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
            SLogs.d("CompositeDisposable dispose");
        }
    }


    @Todo
    public Map<String, RequestBody> genObjRequestBody(Map<String, Object> params) {
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String value = entry.getValue().toString();
            RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), value);
            requestBodyMap.put(entry.getKey(), requestBody);
        }
        return requestBodyMap;
    }

    public Map<String, RequestBody> genRequestBody(Map<String, String> requestDataMap) {
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        if (requestDataMap == null || requestDataMap.isEmpty()) {
            requestBodyMap.put("x-test", RequestBody.create(MediaType.parse("multipart/form-data"), "test"));
            return requestBodyMap;
        }

        for (String key : requestDataMap.keySet()) {
            String s = requestDataMap.get(key);
            if (!TextUtils.isEmpty(s)) {
                RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), s);
                requestBodyMap.put(key, requestBody);
            }
        }
        return requestBodyMap;
    }

    public void addDisposable(@NonNull Disposable closeable) {
        compositeDisposable.add(closeable);
    }

    public <T> void addSingleDisposable(Single<T> single, Consumer<T> consumer) {
        compositeDisposable.add(single
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer, throwable));
    }

    public <T> void addSingleDisposable(Single<T> single, Consumer<T> consumer, Consumer<Throwable> throwable) {
        compositeDisposable.add(single
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer, throwable));
    }

    public <T> void addFlowableDisposable(Flowable<T> flowable, Consumer<T> onNext) {
        compositeDisposable.add(flowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread(), true)
                .subscribe(onNext, throwable));
    }

    public <T> void addFlowableDisposable(Flowable<T> flowable, Consumer<T> onNext, Action onComplete) {
        compositeDisposable.add(flowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNext, throwable, onComplete));
    }

    public <T> void addFlowableDisposable(Flowable<T> flowable, Consumer<T> onNext, Consumer<Throwable> throwable) {
        compositeDisposable.add(flowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNext, throwable));
    }

    public <T> void addFlowableDisposable(Flowable<T> flowable, Consumer<T> onNext, Consumer<Throwable> throwable, Action onComplete) {
        compositeDisposable.add(flowable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNext, throwable, onComplete));
    }

    public <T> void addCompletableDisposable(Completable completable, Action action) {
        compositeDisposable.add(completable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action, throwable));
    }

    public <T> void addCompletableDisposable(Completable completable, Action action, Consumer<Throwable> throwable1) {
        compositeDisposable.add(completable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action, throwable1));
    }


    private void checkException(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;

            if (httpException.getCause() instanceof SSLHandshakeException) {
                getExceptionLiveData().setValue(new kotlin.Pair<>(httpException.code(), SeafException.SSL_EXCEPTION));
            } else {
                getExceptionLiveData().setValue(new kotlin.Pair<>(httpException.code(), SeafException.NETWORK_EXCEPTION));
            }
        }
    }


    private SeafException returnBadRequest(int code) {
        if (HttpURLConnection.HTTP_BAD_REQUEST == code) {
            return SeafException.REQUEST_EXCEPTION;
        }

        return SeafException.NETWORK_EXCEPTION;
    }

    public SeafException getExceptionByThrowable(Throwable throwable) throws IOException {
        return ExceptionUtils.getExceptionByThrowable(throwable);
    }

    public SeafException getExceptionByThrowableForLogin(Throwable throwable, boolean withAuthToken) throws IOException {
        if (throwable == null) {
            return SeafException.UNKNOWN_EXCEPTION;
        }

        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;

            Response<?> resp = httpException.response();

            if (resp != null) {
                String otp = resp.headers().get("X-Seafile-OTP");
                if ("required".equals(otp)) {
                    if (withAuthToken) {
                        return SeafException.TWO_FACTOR_AUTH_TOKEN_INVALID_EXCEPTION;
                    } else {
                        return SeafException.TWO_FACTOR_AUTH_TOKEN_MISSING_EXCEPTION;
                    }
                }
            }
        }

        return ExceptionUtils.getExceptionByThrowable(throwable);
    }


    public String getErrorMsgByThrowable(Throwable throwable) {
        if (throwable instanceof SeafException) {
            SeafException seafException = (SeafException) throwable;
            if (seafException.getCode() == SeafException.INVALID_PASSWORD.getCode()) {
                return SeadroidApplication.getAppContext().getString(R.string.wrong_password);
            }
        } else if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            if (httpException.response() != null && httpException.response().errorBody() != null) {

                if (504 == httpException.code()) {
                    return SeadroidApplication.getAppContext().getString(R.string.network_unavailable);
                }

                String json = null;
                try {
                    json = httpException.response().errorBody().string();
                    if (TextUtils.isEmpty(json)) {
                        return SeadroidApplication.getAppContext().getString(R.string.unknow_error);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                if (TextUtils.isEmpty(json)) {
                    return SeadroidApplication.getAppContext().getString(R.string.unknow_error);
                }

                if (json.contains("{\"error_msg\":")) {
                    ResultModel resultModel = GsonUtils.fromJson(json, ResultModel.class);
                    if (TextUtils.equals("Wrong password", resultModel.error_msg)) {
                        return SeadroidApplication.getAppContext().getString(R.string.wrong_password);
                    }

                    return resultModel.error_msg;
                }

                return json;
            }

//            if (httpException.code() == 404) {
//                if (httpException.response() != null && httpException.response().errorBody() != null) {
//
//                }
//            } else if (httpException.code() == 400) {
//
////                {"error_msg":"Wrong password"}
//
//                return SeadroidApplication.getAppContext().getString(R.string.no_permission);
//            }
        } else if (throwable instanceof SSLHandshakeException) {
            SSLHandshakeException sslHandshakeException = (SSLHandshakeException) throwable;
            SLogs.e(sslHandshakeException.getMessage());
            return SeadroidApplication.getAppContext().getString(R.string.network_unavailable);
        } else if (throwable instanceof SocketTimeoutException) {
            SocketTimeoutException socketTimeoutException = (SocketTimeoutException) throwable;
            SLogs.e(socketTimeoutException.getMessage());
            return SeadroidApplication.getAppContext().getString(R.string.network_unavailable);
        }

        return SeadroidApplication.getAppContext().getString(R.string.unknow_error);
    }
}
