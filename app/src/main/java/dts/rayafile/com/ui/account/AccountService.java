package dts.rayafile.com.ui.account;

import dts.rayafile.com.account.AccountInfo;
import dts.rayafile.com.framework.data.model.TokenModel;

import java.util.Map;

import io.reactivex.Single;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AccountService {


    @POST("api2/device-wiped//")
    @Multipart
    Single<Object> deviceWiped();

    @POST("api2/auth-token/")
    @Multipart
    Call<TokenModel> login(@HeaderMap Map<String, String> headers, @PartMap Map<String, RequestBody> map);

    @GET("api2/account/info/")
    Single<AccountInfo> getAccountInfo();

    @GET("api2/account/info/")
    Call<AccountInfo> getAccountInfoCall();
}
