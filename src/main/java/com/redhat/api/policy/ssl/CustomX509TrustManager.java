package com.redhat.api.policy.ssl;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CustomX509TrustManager implements X509TrustManager {
    private static final String JAVA_TRUST_STORE_PWD_SYS_PROP = "javax.net.ssl.trustStorePassword";
    private static final String JAVA_TRUST_STORE_SYS_PROP = "javax.net.ssl.trustStore";

    private X509TrustManager defaultTrustManager;
    private String myTrustStorePath;
    private String myTrustStorePassphrase;
    private KeyStore myTrustStore;

    public CustomX509TrustManager(KeyStore myTrustStore, String myTrustStorePath, String myTrustStorePassphrase) throws GeneralSecurityException {
        this.myTrustStore = myTrustStore;
        this.myTrustStorePassphrase = myTrustStorePassphrase;
        this.myTrustStorePath = myTrustStorePath;

        fetchDefaultX509TrustManager();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // If you're planning to use client-cert auth, do the same as checking the server.
        this.defaultTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            this.defaultTrustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException ex) {
            String defaultTrustStorePath = getMyTrustStorePath();
            String defaultTrustStorePassphrase = getMyTrustStorePassphrase();

            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(getMyTrustStore());
                for (TrustManager mgr : trustManagerFactory.getTrustManagers()) {
                    if (mgr instanceof X509TrustManager) {
                        ((X509TrustManager) mgr).checkServerTrusted(chain, authType);
                    }
                }
            } catch (KeyStoreException | NoSuchAlgorithmException e) {
                throw new CertificateException("Unable to fetch X509 Trust Manager", e);
            } finally {
                System.setProperty(JAVA_TRUST_STORE_SYS_PROP, defaultTrustStorePath);
                System.setProperty(JAVA_TRUST_STORE_PWD_SYS_PROP, defaultTrustStorePassphrase);
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.defaultTrustManager.getAcceptedIssuers();
    }

    private void fetchDefaultX509TrustManager() throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        for (TrustManager mgr : trustManagerFactory.getTrustManagers()) {
            if (mgr instanceof X509TrustManager) {
                this.defaultTrustManager = (X509TrustManager) mgr;
            }
        }
    }

    public X509TrustManager getDefaultTrustManager() {
        return defaultTrustManager;
    }

    public void setDefaultTrustManager(X509TrustManager defaultTrustManager) {
        this.defaultTrustManager = defaultTrustManager;
    }

    public String getMyTrustStorePath() {
        return myTrustStorePath;
    }

    public void setMyTrustStorePath(String myTrustStorePath) {
        this.myTrustStorePath = myTrustStorePath;
    }

    public String getMyTrustStorePassphrase() {
        return myTrustStorePassphrase;
    }

    public void setMyTrustStorePassphrase(String myTrustStorePassphrase) {
        this.myTrustStorePassphrase = myTrustStorePassphrase;
    }

    public KeyStore getMyTrustStore() {
        return myTrustStore;
    }

    public void setMyTrustStore(KeyStore myTrustStore) {
        this.myTrustStore = myTrustStore;
    }
}
