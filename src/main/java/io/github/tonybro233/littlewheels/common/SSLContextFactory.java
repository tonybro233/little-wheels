package io.github.tonybro233.littlewheels.common;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * SSLContext样板代码
 * 使用时请修改静态成员的值
 */
public class SSLContextFactory {

    private static final String PROTOCOL = "SSLv3";

    private static final String KEY_STORE_TYPE = "PKCS12";

    private static final String KEY_PASSWORD = "Your Password";

    private static final String KEY_ALGO = "SunX509";

    private static final String SERVER_KEY_PATH = "/ssl/server-sample.jks";

    private static final String CLIENT_KEY_PATH = "/ssl/client-sample.jks";

    public static SSLContext newContext(boolean server) {
        InputStream is = null;
        try {
            SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
            KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);
            is = server ? SSLContextFactory.class.getResourceAsStream(SERVER_KEY_PATH) :
                    SSLContextFactory.class.getResourceAsStream(CLIENT_KEY_PATH);
            if (null == is) {
                throw new FileNotFoundException("Key store file not found!");
            }

            ks.load(is, KEY_PASSWORD.toCharArray());

            KeyManager[] keyManagers = getKeyManagers(ks);
            TrustManager[] trustManagers = getTrustManagers(ks);
            if (keyManagers != null && trustManagers != null) {
                sslContext.init(keyManagers, trustManagers, null);
            }
            sslContext.createSSLEngine().getSupportedCipherSuites();

            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Create SSLContext failed", e);
        } finally {
            if(is != null ){
                try {
                    is.close() ;
                } catch (IOException ignore) { }
            }
        }
    }

    private SSLContextFactory(){
    }

    private static TrustManager[] getTrustManagers(KeyStore ks){
        TrustManager[] kms = null ;
        try {
            // 获得KeyManagerFactory对象. 初始化位默认算法
            TrustManagerFactory keyFac = TrustManagerFactory.getInstance(KEY_ALGO) ;
            keyFac.init(ks) ;
            kms = keyFac.getTrustManagers();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kms;
    }

    private static KeyManager[] getKeyManagers(KeyStore ks){
        KeyManager[] kms = null ;
        try {
            // 获得KeyManagerFactory对象. 初始化位默认算法
            KeyManagerFactory keyFac = KeyManagerFactory.getInstance(KEY_ALGO) ;
            keyFac.init(ks, KEY_PASSWORD.toCharArray()) ;
            kms = keyFac.getKeyManagers() ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kms ;
    }
}
