package dts.rayafile.com.framework.util;

import com.blankj.utilcode.util.TimeUtils;
import dts.rayafile.com.config.DateFormatType;

import java.util.Date;

public class Times {
    public static long convertMtime2Long(String mtime) {
        Date date = TimeUtils.string2Date(mtime, DateFormatType.DATE_XXX);
        return TimeUtils.date2Millis(date);
    }
}
