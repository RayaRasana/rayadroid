package dts.rayafile.com.ui.repo;

import dts.rayafile.com.framework.data.db.entities.RepoModel;
import dts.rayafile.com.framework.data.model.dirents.DirentRecursiveFileModel;
import dts.rayafile.com.framework.data.model.permission.PermissionListWrapperModel;
import dts.rayafile.com.framework.data.model.permission.PermissionWrapperModel;
import dts.rayafile.com.framework.data.model.repo.DirentWrapperModel;
import dts.rayafile.com.framework.data.model.repo.RepoWrapperModel;

import java.util.List;

import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RepoService {
    @GET("api/v2.1/repos/")
    Single<RepoWrapperModel> getReposAsync();

    @GET("api/v2.1/repos/")
    Call<RepoWrapperModel> getReposSync();

    @GET("api2/repos/{repo_id}/")
    Single<RepoModel> getRepoInfo(@Path("repo_id") String repoId);

    @GET("api/v2.1/repos/{repo_id}/dir/?with_thumbnail=true")
    Single<DirentWrapperModel> getDirentsAsync(@Path("repo_id") String repoId, @Query("p") String path);

    @GET("api/v2.1/repos/{repo_id}/dir/?with_thumbnail=true")
    Call<DirentWrapperModel> getDirentsSync(@Path("repo_id") String repoId, @Query("p") String path);

    @GET("api2/repos/{repo_id}/dir/?t=f&recursive=1")
    Call<List<DirentRecursiveFileModel>> getDirRecursiveFileCall(@Path("repo_id") String repoId, @Query("p") String path);


    @GET("api/v2.1/repos/{repo_id}/custom-share-permissions/{permission_id}/")
    Single<PermissionWrapperModel> getCustomSharePermissionById(@Path("repo_id") String repoId, @Path("permission_id") int id);

    @GET("api/v2.1/repos/{repo_id}/custom-share-permissions/")
    Single<PermissionListWrapperModel> getCustomSharePermissions(@Path("repo_id") String repoId);
}
