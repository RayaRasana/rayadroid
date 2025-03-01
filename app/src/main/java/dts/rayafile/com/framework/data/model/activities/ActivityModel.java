package dts.rayafile.com.framework.data.model.activities;

import android.text.TextUtils;

import dts.rayafile.com.framework.data.db.entities.DirentModel;
import dts.rayafile.com.framework.data.model.BaseModel;
import dts.rayafile.com.enums.OpType;
import dts.rayafile.com.framework.util.Times;
import dts.rayafile.com.framework.util.Utils;

public class ActivityModel extends BaseModel {
    public String op_type;
    public String repo_id;
    public String repo_name;
    public String obj_type;
    public String commit_id;
    public String path;
    public String name;
    public String old_path;
    public String old_name;
    public String author_email;
    public String author_name;
    public String author_contact_email;
    public String avatar_url;
    public String time;

    private long mTimeLong;
    public OpType opType;

    public String getTime() {
        if (mTimeLong == 0){
            mTimeLong = Times.convertMtime2Long(time);
        }
        return Utils.translateCommitTime(mTimeLong);
    }


    public boolean isFileOpenable() {
        return opType == OpType.CREATE ||
                opType == OpType.UPDATE ||
                opType == OpType.RENAME ||
                opType == OpType.EDIT;
    }

    public boolean isDir() {
        return TextUtils.equals(obj_type,"dir");
    }


    public static DirentModel convert2DirentModel(ActivityModel model){
        DirentModel d = new DirentModel();
        d.full_path = model.path;
        d.type = model.obj_type;
        d.mtime = 0;
        d.name = model.name;
        d.repo_id = model.repo_id;
        d.repo_name = model.repo_name;
        d.parent_dir = Utils.getParentPath(model.path);
        return d;
    }
}
