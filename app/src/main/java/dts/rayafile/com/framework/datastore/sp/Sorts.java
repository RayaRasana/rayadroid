package dts.rayafile.com.framework.datastore.sp;

import android.text.TextUtils;

import dts.rayafile.com.framework.datastore.DataStoreManager;

@Deprecated
public class Sorts {
    /**
     * sort files type
     */
    public static final String SORT_KEY = "sort_by_name";
    /**
     * sort files type
     */
    public static final String SORT_ORDER = "sort_by_modified_time";

    /**
     * sort files type
     */
    public static final String BY_NAME = "sort_by_name";//sort_by_name/sort_by_modified_time
    /**
     * sort files type
     */
    public static final String BY_MODIFIED_TIME = "sort_by_modified_time";
    /**
     * sort files order
     */
    public static final String ASCENDING = "ascending";
    /**
     * sort files order
     */
    public static final String DESCENDING = "descending";

    public static final int SORT_BY_NAME_ASC = 0;
    public static final int SORT_BY_NAME_DESC = 1;
    public static final int SORT_BY_MODIFIED_TIME_ASC = 2;
    public static final int SORT_BY_MODIFIED_TIME_DESC = 3;

    public static void init() {
        String k = DataStoreManager.getCommonSharePreference().readString(SORT_KEY);
        if (TextUtils.isEmpty(k)) {
            DataStoreManager.getCommonSharePreference().writeString(SORT_KEY, BY_NAME);
        }

        String o = DataStoreManager.getCommonSharePreference().readString(SORT_ORDER);
        if (TextUtils.isEmpty(o)) {
            DataStoreManager.getCommonSharePreference().writeString(SORT_ORDER, DESCENDING);
        }
    }

    public static String getSortKey() {
        return DataStoreManager.getCommonSharePreference().readString(SORT_KEY);
    }

    public static String getSortOrder() {
        return DataStoreManager.getCommonSharePreference().readString(SORT_ORDER);
    }

    public static void setSortType(int type) {
        if (type == SORT_BY_NAME_ASC) {
            DataStoreManager.getCommonSharePreference().writeString(SORT_KEY, BY_NAME);
            DataStoreManager.getCommonSharePreference().writeString(SORT_ORDER, ASCENDING);
        } else if (type == SORT_BY_NAME_DESC) {
            DataStoreManager.getCommonSharePreference().writeString(SORT_KEY, BY_NAME);
            DataStoreManager.getCommonSharePreference().writeString(SORT_ORDER, DESCENDING);
        } else if (type == SORT_BY_MODIFIED_TIME_ASC) {
            DataStoreManager.getCommonSharePreference().writeString(SORT_KEY, BY_MODIFIED_TIME);
            DataStoreManager.getCommonSharePreference().writeString(SORT_ORDER, ASCENDING);
        } else if (type == SORT_BY_MODIFIED_TIME_DESC) {
            DataStoreManager.getCommonSharePreference().writeString(SORT_KEY, BY_MODIFIED_TIME);
            DataStoreManager.getCommonSharePreference().writeString(SORT_ORDER, DESCENDING);
        }
    }

    public static int getSortType() {
        String order = getSortOrder();
        switch (getSortKey()) {
            case BY_NAME:
                if (order.equals(ASCENDING))
                    return SORT_BY_NAME_ASC;
                else if (order.equals(DESCENDING))
                    return SORT_BY_NAME_DESC;
                break;
            case BY_MODIFIED_TIME:
                if (order.equals(ASCENDING))
                    return SORT_BY_MODIFIED_TIME_ASC;
                else if (order.equals(DESCENDING))
                    return SORT_BY_MODIFIED_TIME_DESC;
                break;
        }
        return SORT_BY_NAME_ASC;
    }
}
