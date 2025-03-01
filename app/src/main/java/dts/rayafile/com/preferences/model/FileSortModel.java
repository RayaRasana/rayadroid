package dts.rayafile.com.preferences.model;

import dts.rayafile.com.enums.SortBy;
import dts.rayafile.com.enums.SortOrder;
import dts.rayafile.com.framework.data.model.BaseModel;

import java.util.Comparator;

import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.functions.Function1;

public class FileSortModel {
    public SortBy by;
    public SortOrder order;
    public boolean isFolderFirst;

    public FileSortModel(SortBy by, SortOrder order, boolean isFolderFirst) {
        this.by = by;
        this.order = order;
        this.isFolderFirst = isFolderFirst;
    }

    /**
     *
     */
    public Comparator<BaseModel> createComparator() {
        Comparator<BaseModel> comparator = ComparisonsKt.compareBy(new Function1<BaseModel, Comparable<BaseModel>>() {
            @Override
            public Comparable<BaseModel> invoke(BaseModel baseModel) {
                return null;
            }
        }).thenComparing(new Comparator<BaseModel>() {
            @Override
            public int compare(BaseModel o1, BaseModel o2) {
                return 0;
            }
        });

        if (SortBy.NAME == by) {

        } else if (SortBy.TYPE == by) {

        } else if (SortBy.SIZE == by) {

        } else if (SortBy.LAST_MODIFIED == by) {

        }
        return null;
    }
}
