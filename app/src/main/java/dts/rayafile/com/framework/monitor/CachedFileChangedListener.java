package dts.rayafile.com.framework.monitor;

import dts.rayafile.com.account.Account;
import dts.rayafile.com.framework.data.SeafCachedFile;

import java.io.File;

interface CachedFileChangedListener {
    void onCachedBlocksChanged(Account account, SeafCachedFile cf, File file);

    void onCachedFileChanged(Account account, SeafCachedFile cf, File file);
}

