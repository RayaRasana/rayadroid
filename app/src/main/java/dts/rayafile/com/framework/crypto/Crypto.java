package dts.rayafile.com.framework.crypto;

import androidx.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import dts.rayafile.com.SeafException;

import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * There are a few ways to derive keys, but most of them are not particularly secure.
 * To ensure encryption keys are both sufficiently random and hard to brute force, we should use standard PBE key derivation methods.
 * Other Seafile platforms, e.g, server side, using PBKDF2WithHmacSHA256 to derive a key/iv pair from the password,
 * using AES 256/CBC to encrypt the data.
 * <p/>
 * Unfortunately, Android SDK doesn`t support PBKDF2WithHmacSHA256, so we use Spongy Castle, which is the stock Bouncy Castle libraries with a couple of small changes to make it work on Android.
 * For version 1.47 or higher of SpongyCastle, we can invoke PBKDF2WithHmacSHA256 directly,
 * but for versions below 1.47, we could not specify SHA256 digest and it defaulted to SHA1.
 * see
 * 1. https://rtyley.github.io/spongycastle/
 * 2. http://stackoverflow.com/a/15303291/3962551
 * 3. https://en.wikipedia.org/wiki/Bouncy_Castle_(cryptography)
 */
public class Crypto {
    private static final String TAG = Crypto.class.getSimpleName();

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final String CHAR_SET = "UTF-8";

    private static int KEY_LENGTH = 32;
    private static int KEY_LENGTH_SHORT = 16;
    private static int ITERATION_COUNT = 1000;
    // Should generate random salt for each repo
    private static byte[] salt = {(byte) 0xda, (byte) 0x90, (byte) 0x45, (byte) 0xc3, (byte) 0x06, (byte) 0xc7, (byte) 0xcc, (byte) 0x26};

    static {
        // http://stackoverflow.com/questions/6898801/how-to-include-the-spongy-castle-jar-in-android
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private Crypto() {
    }

    /**
     * When you view an encrypted library, the client needs to verify your password.
     * When you create the library, a "magic token" is derived from the library id and password.
     * This token is stored with the library on the server side.
     * <p/>
     * The client use this token to check whether your password is correct before you view the library.
     * The magic token is generated by PBKDF2 algorithm with 1000 iterations of SHA256 hash.
     *
     * @param repoID
     * @param password
     * @param version
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private static byte[] generateMagic(String repoID, String password, int version) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException, SeafException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (version != 1 && version != 2) {
            throw SeafException.UNSUPPORTED_ENC_VERSION;
        }

        return deriveKey(repoID + password, version);
    }

    /**
     * Recompute the magic and compare it with the one comes with the repo.
     *
     * @param repoId
     * @param password
     * @param version
     * @param magic
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws UnsupportedEncodingException
     * @throws SeafException
     */
    public static void verifyRepoPassword(String repoId, String password, int version, String magic) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException, SeafException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        final byte[] generateMagic = generateMagic(repoId, password, version);
        final byte[] genMagic = toHex(generateMagic).getBytes(CHAR_SET);
        final byte[] repoMagic = magic.getBytes(CHAR_SET);
        int diff = genMagic.length ^ repoMagic.length;
        for (int i = 0; i < genMagic.length && i < repoMagic.length; i++) {
            diff |= genMagic[i] ^ repoMagic[i];
        }

        if (diff != 0) throw SeafException.INVALID_PASSWORD;
    }

    /**
     * First use PBKDF2 algorithm (1000 iteratioins of SHA256) to derive a key/iv pair from the password,
     * then use AES 256/CBC to decrypt the "file key" from randomKey (the "encrypted file key").
     * The client only saves the key/iv pair derived from the "file key", which is used to decrypt the data.
     *
     * @param password
     * @param randomKey encrypted file key
     * @param version
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public static Pair<String, String> generateKey(@NonNull String password, @NonNull String randomKey, int version) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // derive a key/iv pair from the password
        final byte[] key = deriveKey(password, version);
        SecretKey derivedKey = new SecretKeySpec(key, "AES");
        final byte[] iv = deriveIv(key);

        // decrypt the file key from the encrypted file key
        final byte[] fileKey = seafileDecrypt(fromHex(randomKey), derivedKey, iv);
        // The client only saves the key/iv pair derived from the "file key", which is used to decrypt the data
        final String encKey = deriveKey(fileKey, version);
        return new Pair<>(encKey, toHex(deriveIv(fromHex(encKey))));
    }

    /**
     * Derive secret key by PBKDF2 algorithm (1000 iterations of SHA256)
     *
     * @param password
     * @param version
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    private static byte[] deriveKey(@NonNull String password, int version) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toCharArray()), salt, ITERATION_COUNT);
        return ((KeyParameter) gen.generateDerivedMacParameters(version == 2 ? KEY_LENGTH * 8 : KEY_LENGTH_SHORT * 8)).getKey();
    }

    /**
     * Derive secret key by PBKDF2 algorithm (1000 iterations of SHA256)
     *
     * @param fileKey
     * @param version
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    private static String deriveKey(@NonNull byte[] fileKey, int version) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        try {
            PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
            gen.init(fileKey, salt, ITERATION_COUNT);
            byte[] keyBytes = ((KeyParameter) gen.generateDerivedMacParameters(version == 2 ? KEY_LENGTH * 8 : KEY_LENGTH_SHORT * 8)).getKey();
            return toHex(keyBytes);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(" Attempt to get length of null array");
        }
    }

    /**
     * Derive initial vector by PBKDF2 algorithm (10 iterations of SHA256)
     *
     * @param key
     * @return
     * @throws UnsupportedEncodingException
     */
    private static byte[] deriveIv(@NonNull byte[] key) throws UnsupportedEncodingException {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(key, salt, 10);
        return ((KeyParameter) gen.generateDerivedMacParameters(KEY_LENGTH_SHORT * 8)).getKey();
    }

    /**
     * Do the decryption
     *
     * @param bytes
     * @param key
     * @param iv
     * @return
     */
    private static byte[] seafileDecrypt(@NonNull byte[] bytes, @NonNull SecretKey key, @NonNull byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            return cipher.doFinal(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "NoSuchAlgorithmException " + e.getMessage());
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            Log.e(TAG, "InvalidKeyException " + e.getMessage());
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "NoSuchPaddingException " + e.getMessage());
            return null;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "seafileDecrypt BadPaddingException " + e.getMessage());
            return null;
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG, "IllegalBlockSizeException " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG, "InvalidAlgorithmParameterException " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Do the encryption
     *
     * @param plaintext
     * @param inputLen
     * @param key
     * @param iv
     * @return
     */
    private static byte[] seafileEncrypt(@NonNull byte[] plaintext, int inputLen, @NonNull SecretKey key, @NonNull byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
            return cipher.doFinal(plaintext, 0, inputLen);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "NoSuchAlgorithmException " + e.getMessage());
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            Log.e(TAG, "InvalidKeyException " + e.getMessage());
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            Log.e(TAG, "InvalidAlgorithmParameterException " + e.getMessage());
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "NoSuchPaddingException " + e.getMessage());
            return null;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            Log.e(TAG, "IllegalBlockSizeException " + e.getMessage());
            return null;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "seafileEncrypt BadPaddingException " + e.getMessage());
            return null;
        }
    }

    /**
     * All file data is encrypted by the encKey/encIv with AES 256/CBC.
     *
     * @param plaintext
     * @param encKey
     * @param iv
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static byte[] encrypt(@NonNull byte[] plaintext, @NonNull String encKey, @NonNull String iv) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return encrypt(plaintext, plaintext.length, encKey, iv);
    }

    /**
     * All file data is encrypted by the encKey/encIv with AES 256/CBC.
     *
     * @param plaintext
     * @param inputLen
     * @param encKey
     * @param iv
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static byte[] encrypt(@NonNull byte[] plaintext, int inputLen, @NonNull String encKey, @NonNull String iv) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SecretKey secretKey = new SecretKeySpec(fromHex(encKey), "AES");
        return seafileEncrypt(plaintext, inputLen, secretKey, fromHex(iv));
    }

    /**
     * All file data is decrypted by the encKey/encIv with AES 256/CBC.
     *
     * @param plaintext
     * @param encKey
     * @param iv
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static byte[] decrypt(@NonNull byte[] plaintext, @NonNull String encKey, @NonNull String iv) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SecretKey realKey = new SecretKeySpec(fromHex(encKey), "AES");
        return seafileDecrypt(plaintext, realKey, fromHex(iv));
    }

    /**
     * Convert byte to Hexadecimal
     *
     * @param buf
     * @return
     */
    private static String toHex(@NonNull byte[] buf) {
        if (buf == null) return "";

        String hex = "0123456789abcdef";

        StringBuilder result = new StringBuilder(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            result.append(hex.charAt((buf[i] >> 4) & 0x0f)).append(hex.charAt(buf[i] & 0x0f));

        }
        return result.toString();
    }

    /**
     * Convert Hexadecimal to byte
     *
     * @param hex
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] fromHex(@NonNull String hex) throws NoSuchAlgorithmException {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    public static String toBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    public static String sha1(@NonNull byte[] cipher) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(cipher, 0, cipher.length);
        return toHex(md.digest());
    }
}
