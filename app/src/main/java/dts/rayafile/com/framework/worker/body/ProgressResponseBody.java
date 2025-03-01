package dts.rayafile.com.framework.worker.body;

import dts.rayafile.com.listener.FileTransferProgressListener;
import dts.rayafile.com.listener.ProgressListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class ProgressResponseBody extends ResponseBody {
    private final ResponseBody responseBody;
    private BufferedSource bufferedSource;
    private FileTransferProgressListener fileTransferProgressListener;


    public ProgressResponseBody(ResponseBody responseBody, FileTransferProgressListener fileTransferProgressListener) {
        this.responseBody = responseBody;
        this.fileTransferProgressListener = fileTransferProgressListener;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    public long temp = System.currentTimeMillis();

    private ForwardingSource source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;


                long nowt = System.currentTimeMillis();
                // 1s refresh progress
                if (nowt - temp >= 1000) {
                    temp = nowt;
                    if (fileTransferProgressListener != null) {
                        fileTransferProgressListener.onProgressNotify(totalBytesRead, responseBody.contentLength());
                    }
                }

                return bytesRead;
            }
        };
    }
}
