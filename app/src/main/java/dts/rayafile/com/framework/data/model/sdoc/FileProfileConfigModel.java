package dts.rayafile.com.framework.data.model.sdoc;

import dts.rayafile.com.framework.data.model.user.UserWrapperModel;

public class FileProfileConfigModel {
    public UserWrapperModel users;
    public FileDetailModel detail;
    public MetadataConfigModel metadataConfigModel;

    public MetadataConfigModel getMetadataConfigModel() {
        return metadataConfigModel;
    }

    public void setMetadataConfigModel(MetadataConfigModel metadataConfigModel) {
        this.metadataConfigModel = metadataConfigModel;
    }

    public UserWrapperModel getUsers() {
        return users;
    }

    public void setUsers(UserWrapperModel users) {
        this.users = users;
    }

    public FileDetailModel getDetail() {
        return detail;
    }

    public void setDetail(FileDetailModel detail) {
        this.detail = detail;
    }
}
