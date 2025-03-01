package dts.rayafile.com.framework.data.model.repo;

import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.framework.data.model.ResultModel;

import java.util.ArrayList;
import java.util.List;

public class DirentWrapperModel extends ResultModel {
    public String user_perm;
    public String dir_id;
    public List<DirentModel> dirent_list = new ArrayList<>();
}
