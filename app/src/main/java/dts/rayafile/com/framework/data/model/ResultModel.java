package dts.rayafile.com.framework.data.model;

public class ResultModel {
    public boolean success;
    public String error_msg;

    @Override
    public String toString() {
        return "ResultModel{" +
                "success=" + success +
                ", error_msg='" + error_msg + '\'' +
                '}';
    }
}
