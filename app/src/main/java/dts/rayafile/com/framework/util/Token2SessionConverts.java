package dts.rayafile.com.framework.util;

import dts.rayafile.com.account.Account;
import dts.rayafile.com.account.SupportAccountManager;

public class Token2SessionConverts {

    public static String buildUrl(String next) {
        Account account = SupportAccountManager.getInstance().getCurrentAccount();
        if (account == null) {
            return next;
        }
        String host = account.server;
        return host + "mobile-login/?next=" + next;
    }
}
