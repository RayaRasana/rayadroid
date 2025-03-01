package dts.rayafile.com.framework.data.model.server;

import com.blankj.utilcode.util.CollectionUtils;

import java.util.List;

public class ServerInfoModel {
    public String version;
    public String encrypted_library_version;

    //pbkdf2_sha256
    public String encrypted_library_pwd_hash_algo = null;
    //1000
    public String encrypted_library_pwd_hash_params = null;

    public List<String> features;

    public String getFeaturesString() {
        if (CollectionUtils.isEmpty(features)) {
            return null;
        }

        return String.join(",", features);
    }
}
