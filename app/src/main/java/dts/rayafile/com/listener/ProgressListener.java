package dts.rayafile.com.listener;

public interface ProgressListener {
    void onProgress(String fileName, long cur, long total);

    boolean isCancelled();
}
