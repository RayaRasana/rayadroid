package dts.rayafile.com.framework.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import dts.rayafile.com.framework.data.db.entities.PermissionEntity;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface PermissionDAO {

    @Query("select * from permissions where id = :id limit 1")
    List<PermissionEntity> getByIdSync(int id);

    @Query("select * from permissions where id IN (:ids)")
    Single<List<PermissionEntity>> getByIdsAsync(List<Integer> ids);

    @Query("select * from permissions where repo_id = :repoId")
    Single<List<PermissionEntity>> getByRepoIdAsync(String repoId);

    @Query("select * from permissions where repo_id = :repoId and id = :pid limit 1")
    Single<List<PermissionEntity>> getByRepoAndIdAsync(String repoId, int pid);


    @Query("DELETE FROM permissions")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PermissionEntity PermissionEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<PermissionEntity> list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertAllAsync(List<PermissionEntity> list);


}
