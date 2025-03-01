package dts.rayafile.com.framework.data.model.sdoc;

import dts.rayafile.com.framework.data.model.BaseModel;

import java.util.List;

public class OutlineItemModel extends BaseModel {
    public String id;
    public String type;
    public String text;
    public boolean indent;
    public List<OutlineItemModel> children;
    public SDocDataModel data;
}
