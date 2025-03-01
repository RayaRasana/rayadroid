package dts.rayafile.com.ui.activities;

import dts.rayafile.com.framework.data.model.activities.ActivityWrapperModel;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ActivityService {
    @GET("api/v2.1/activities/?avatar_size=72")
    Single<ActivityWrapperModel> getActivities(@Query("page") int page);
}
