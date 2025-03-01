package dts.rayafile.com.ssl;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;

import com.blankj.utilcode.util.EncryptUtils;
import com.google.common.collect.Maps;
import dts.rayafile.com.framework.datastore.DataStoreKeys;
import dts.rayafile.com.account.Account;
import dts.rayafile.com.preferences.Settings;

/**
 * Save the ssl certificates the user has confirmed to trust
 */
public final class CertsManager {

    private final Map<String, X509Certificate> cachedCerts = Maps.newConcurrentMap();

    private static CertsManager instance;

    public static synchronized CertsManager instance() {
        if (instance == null) {
            instance = new CertsManager();
        }

        return instance;
    }

    public void saveCertForAccount(final Account account, boolean rememberChoice) {
        List<X509Certificate> certs = SSLTrustManager.instance().getCertsChainForAccount(account);
        if (certs == null || certs.isEmpty()) {
            return;
        }

        final X509Certificate cert = certs.get(0);
        cachedCerts.put(account.getServer(), cert);

        if (rememberChoice) {
            // save cert info to shared preferences
            String certBase64 = CertsHelper.getCertBase64(cert);
            String keyPrefix = EncryptUtils.encryptMD5ToString(account.getServer());
            Settings.getCommonPreferences().edit().putString(DataStoreKeys.KEY_SERVER_CERT_INFO + "_" + keyPrefix, certBase64).apply();
        }
    }

    public void deleteCertForAccount(final Account account) {
        if (account == null) {
            return;
        }

        cachedCerts.remove(account.getServer());

        String keyPrefix = EncryptUtils.encryptMD5ToString(account.getServer());
        Settings.getCommonPreferences().edit().remove(DataStoreKeys.KEY_SERVER_CERT_INFO + "_" + keyPrefix).apply();
    }

    public X509Certificate getCertificate(Account account) {
        X509Certificate cert = cachedCerts.get(account.getServer());
        if (cert != null) {
            return cert;
        }

        String keyPrefix = EncryptUtils.encryptMD5ToString(account.getServer());
        String certBase64 = Settings.getCommonPreferences().getString(DataStoreKeys.KEY_SERVER_CERT_INFO + "_" + keyPrefix, null);
        if (TextUtils.isEmpty(certBase64)) {
            return null;
        }

        cert = CertsHelper.convertToCert(certBase64);
        if (cert != null) {
            cachedCerts.put(account.getServer(), cert);
        }

        return cert;
    }
}
