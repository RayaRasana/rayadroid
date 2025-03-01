package dts.rayafile.com.framework.data.model.repo;

import dts.rayafile.com.framework.data.db.entities.PermissionEntity;
import dts.rayafile.com.framework.data.db.entities.RepoModel;

public class RepoPermissionWrapper {
    public RepoModel repoModel;
    public PermissionEntity permission;

    public RepoPermissionWrapper(RepoModel repoModel, PermissionEntity permission) {
        this.repoModel = repoModel;
        this.permission = permission;
    }

    public PermissionEntity getPermission() {
        return permission;
    }

    public void setPermission(PermissionEntity permission) {
        this.permission = permission;
    }

    public void setRepoModel(RepoModel repoModel) {
        this.repoModel = repoModel;
    }

    public RepoModel getRepoModel() {
        return repoModel;
    }
}
