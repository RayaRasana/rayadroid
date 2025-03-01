package dts.rayafile.com.ui.account;

import android.os.Build;
import android.text.TextUtils;
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;

import com.blankj.utilcode.util.AppUtils;
import dts.rayafile.com.SeafException;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.AccountInfo;
import dts.rayafile.com.account.SupportAccountManager;
import dts.rayafile.com.framework.data.ServerInfo;
import dts.rayafile.com.framework.data.model.TokenModel;
import dts.rayafile.com.framework.data.model.server.ServerInfoModel;
import dts.rayafile.com.framework.http.HttpIO;
import dts.rayafile.com.account.AccountUtils;
import dts.rayafile.com.framework.util.DeviceIdManager;
import dts.rayafile.com.ssl.CertsManager;
import dts.rayafile.com.ui.base.viewmodel.BaseViewModel;
import dts.rayafile.com.ui.main.MainService;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Consumer;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;

public class AccountViewModel extends BaseViewModel {
    private final MutableLiveData<Account> mAccountLiveData = new MutableLiveData<>();
    private final MutableLiveData<AccountInfo> mAccountInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<Pair<Account, SeafException>> AccountSeafExceptionLiveData = new MutableLiveData<>();
    private final MutableLiveData<ServerInfo> ServerInfoLiveData = new MutableLiveData<>();

    public MutableLiveData<ServerInfo> getServerInfoLiveData() {
        return ServerInfoLiveData;
    }

    public MutableLiveData<Pair<Account, SeafException>> getAccountSeafExceptionLiveData() {
        return AccountSeafExceptionLiveData;
    }

    public MutableLiveData<Account> getAccountLiveData() {
        return mAccountLiveData;
    }

    public MutableLiveData<AccountInfo> getAccountInfoLiveData() {
        return mAccountInfoLiveData;
    }

    public void loadAccountInfo(Account loginAccount, String authToken) {
        getRefreshLiveData().setValue(true);

        loginAccount.token = authToken;
        Single<AccountInfo> single = HttpIO.getInstanceByAccount(loginAccount).execute(AccountService.class).getAccountInfo();
        addSingleDisposable(single, new Consumer<AccountInfo>() {
            @Override
            public void accept(AccountInfo accountInfo) throws Exception {

                loginAccount.login_time = System.currentTimeMillis();
                loginAccount.setEmail(accountInfo.getEmail());
                loginAccount.setName(accountInfo.getName());
                loginAccount.setAvatarUrl(accountInfo.getAvatarUrl());

                getAccountLiveData().setValue(loginAccount);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);

                SeafException seafException = getExceptionByThrowable(throwable);
                getAccountSeafExceptionLiveData().setValue(new Pair<>(loginAccount, seafException));
            }
        });
    }

    public void login(Account tempAccount, String pwd, String authToken, boolean isRememberDevice) {
        getRefreshLiveData().setValue(true);

        Single<Account> single = Single.create(new SingleOnSubscribe<Account>() {
            @Override
            public void subscribe(SingleEmitter<Account> emitter) throws Exception {

                //the SYNC way

                Call<TokenModel> call = getLoginCall(tempAccount, pwd, authToken, isRememberDevice);
                Response<TokenModel> response = call.execute();
                if (!response.isSuccessful()) {
                    HttpException httpException = new HttpException(response);
                    throw getExceptionByThrowableForLogin(httpException, !TextUtils.isEmpty(authToken));
                }

                String s2fa = response.headers().get("x-seafile-s2fa");
                if (!TextUtils.isEmpty(s2fa)) {
                    tempAccount.sessionKey = s2fa;
                }

                TokenModel tokenModel = response.body();
                if (tokenModel != null) {
                    tempAccount.token = tokenModel.token;
                }

                //
                retrofit2.Response<AccountInfo> accountInfoResponse = HttpIO
                        .getInstanceByAccount(tempAccount) //Still use it that way
                        .execute(AccountService.class)
                        .getAccountInfoCall()
                        .execute();

                if (!accountInfoResponse.isSuccessful()) {
                    HttpException httpException = new HttpException(response);
                    throw getExceptionByThrowable(httpException);
                }

                AccountInfo accountInfo = accountInfoResponse.body();
                if (accountInfo == null) {
                    throw SeafException.NETWORK_EXCEPTION;
                }

                // Update the account info
                tempAccount.setName(accountInfo.getName());
                tempAccount.setEmail(accountInfo.getEmail());
                tempAccount.setAvatarUrl(accountInfo.getAvatarUrl());
                tempAccount.setTotalSpace(accountInfo.getTotal());
                tempAccount.setUsageSpace(accountInfo.getUsage());

                emitter.onSuccess(tempAccount);
            }
        });

        addSingleDisposable(single, new Consumer<Account>() {
            @Override
            public void accept(Account account) throws Exception {
                getRefreshLiveData().setValue(false);
                getAccountLiveData().setValue(account);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);

                SeafException seafException = getExceptionByThrowable(throwable);
                getAccountSeafExceptionLiveData().setValue(new Pair<>(tempAccount, seafException));
            }
        });
    }

    private Call<TokenModel> getLoginCall(Account tempAccount, String pwd, String token, boolean isRememberDevice) {
        Map<String, String> headers = new HashMap<>();

        if (!TextUtils.isEmpty(token)) {
            headers.put("X-Seafile-OTP", token);
        }

        if (!TextUtils.isEmpty(tempAccount.sessionKey)) {
            headers.put("X-SEAFILE-S2FA", tempAccount.sessionKey);
        }

        if (isRememberDevice) {
            headers.put("X-SEAFILE-2FA-TRUST-DEVICE", String.valueOf(1));
        }

        Map<String, String> body = new HashMap<>();
        body.put("username", tempAccount.email);
        body.put("password", pwd);

        String deviceId = DeviceIdManager.getInstance().getOrSet();
        String appVersion = AppUtils.getAppVersionName();

        body.put("platform", "android");
        body.put("device_id", deviceId);
        body.put("device_name", Build.MODEL);
        body.put("client_version", appVersion);
        body.put("platform_version", Build.VERSION.RELEASE);

        Map<String, RequestBody> requestBody = genRequestBody(body);

        return HttpIO.getInstanceByAccount(tempAccount).execute(AccountService.class).login(headers, requestBody);
    }

    public void getServerInfo() {
        getRefreshLiveData().setValue(true);

        Single<ServerInfoModel> serverSingle = HttpIO.getCurrentInstance().execute(MainService.class).getServerInfo();
        addSingleDisposable(serverSingle, new Consumer<ServerInfoModel>() {
            @Override
            public void accept(ServerInfoModel serverInfoModel) {

                Account account = SupportAccountManager.getInstance().getCurrentAccount();
                if (account == null) {
                    return;
                }

                ServerInfo serverInfo1 = new ServerInfo(account.server, serverInfoModel.version, serverInfoModel.getFeaturesString(), serverInfoModel.encrypted_library_version);
                SupportAccountManager.getInstance().setServerInfo(account, serverInfo1);

                getRefreshLiveData().setValue(false);
                getServerInfoLiveData().setValue(serverInfo1);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                getRefreshLiveData().setValue(false);
                getServerInfoLiveData().setValue(null);
            }
        });
    }

    public void deleteAccount(Account account) {
        Account curAccount = SupportAccountManager.getInstance().getCurrentAccount();

        //The user who is currently logged in is deleted
        if (curAccount != null && curAccount.equals(account)) {
            //
            AccountUtils.logout(account);
        } else {
            HttpIO.removeInstanceByAccount(account);
            CertsManager.instance().deleteCertForAccount(account);
        }

        //delete local account
        SupportAccountManager.getInstance().removeAccount(account.getAndroidAccount(), null, null);

    }
}
