package dts.rayafile.com.framework.data;

public interface ProgressMonitor {
    void onProgressNotify(long total, boolean updateTotal);

    boolean isCancelled();
}
