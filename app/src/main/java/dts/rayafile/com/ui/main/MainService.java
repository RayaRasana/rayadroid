package dts.rayafile.com.ui.main;

import dts.rayafile.com.framework.data.model.server.ServerInfoModel;

import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.http.GET;

public interface MainService {

    @GET("api2/server-info/")
    Single<ServerInfoModel> getServerInfo();

    @GET("api2/server-info/")
    Call<ServerInfoModel> getServerInfoSync();
}
