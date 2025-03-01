package dts.rayafile.com.framework.http.callback;

public interface ProgressCallback {
    void onProgress(long transferSize, long totalSize);
}
