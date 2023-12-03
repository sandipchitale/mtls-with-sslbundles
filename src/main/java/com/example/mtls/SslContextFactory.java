package com.example.mtls;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Moritz Halbritter
 */
@Component
class SslContextFactory {
    SSLContext build(SslBundle... bundles) throws KeyManagementException, NoSuchAlgorithmException {
        if (bundles.length == 0) {
            return createEmptySslContext();
        }
        // We use protocol from first SslBundle and assume that all SslBundles use the same
        SSLContext sslContext = SSLContext.getInstance(bundles[0].getProtocol());
        sslContext.init(getKeyManagers(bundles),
            getTrustManagers(bundles),
            null);
        return sslContext;
    }

    SslContext buildReactorSslContext(SslBundle... bundles) throws KeyManagementException, NoSuchAlgorithmException, SSLException {
        if (bundles.length == 0) {
            return createEmptyReactorSslContext();
        }

        // We use protocol from first SslBundle and assume that all SslBundles use the same
        return SslContextBuilder
            .forClient()
            .keyManager(getKeyManagers(bundles)[0])
            .trustManager(getTrustManagers(bundles)[0])
            .build();
    }

    private SSLContext createEmptySslContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance(SslBundle.DEFAULT_PROTOCOL);
        context.init(new KeyManager[0], new TrustManager[0], null);
        return context;
    }

    private SslContext createEmptyReactorSslContext() throws NoSuchAlgorithmException, KeyManagementException, SSLException {
        return SslContextBuilder
            .forClient()
            .trustManager(new TrustManager() {})
            .build();
    }

    private KeyManager[] getKeyManagers(SslBundle... bundles) {
        List<KeyManager> keyManagers = new ArrayList<>();
        for (SslBundle bundle : bundles) {
            keyManagers.addAll(Arrays.asList(bundle.getManagers().getKeyManagers()));
        }
        return keyManagers.toArray(KeyManager[]::new);
    }

    private TrustManager[] getTrustManagers(SslBundle... bundles) {
        List<X509TrustManager> trustManagers = new ArrayList<>();
        for (SslBundle bundle : bundles) {
            for (TrustManager trustManager : bundle.getManagers().getTrustManagers()) {
                if (trustManager instanceof X509TrustManager x509TrustManager) {
                    trustManagers.add(x509TrustManager);
                } else {
                    throw new IllegalStateException("Unsupported trust manager type: " + trustManager.getClass());
                }
            }
        }
        // We wrap trust managers from SslBundles into a single CompositeX509TrustManager
        // Although javax.net.ssl.SSLContext.init allows to pass in multiple TrustManagers,
        // it will only use the first X509TrustManager. This is a X509TrustManager
        // implementation, which delegates to multiple X509TrustManagers.
        return new TrustManager[]{new CompositeX509TrustManager(trustManagers)};
    }

    // Although javax.net.ssl.SSLContext.init allows to pass in multiple TrustManagers,
    // it will only use the first X509TrustManager. This is a X509TrustManager implementation,
    // which delegates to multiple X509TrustManagers.
    private static class CompositeX509TrustManager implements X509TrustManager {
        private final List<X509TrustManager> trustManagers;

        CompositeX509TrustManager(List<X509TrustManager> trustManagers) {
            this.trustManagers = trustManagers;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            for (X509TrustManager trustManager : this.trustManagers) {
                try {
                    trustManager.checkClientTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    // Ignore
                }
            }
            throw new CertificateException("None of the TrustManagers trust this certificate chain");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            for (X509TrustManager trustManager : this.trustManagers) {
                try {
                    trustManager.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    // Ignore
                }
            }
            throw new CertificateException("None of the TrustManagers trust this certificate chain");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> certificates = new ArrayList<>();
            for (X509TrustManager trustManager : this.trustManagers) {
                certificates.addAll(Arrays.asList(trustManager.getAcceptedIssuers()));
            }
            return certificates.toArray(X509Certificate[]::new);
        }
    }

}
