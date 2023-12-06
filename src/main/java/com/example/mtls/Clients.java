package com.example.mtls;

import io.netty.handler.ssl.SslContext;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Moritz Halbritter
 */
@Component
class Clients {

    private final RestClient restClient;
    private final RestClient restClientLoopback;
    private final RestTemplate restTemplate;
    private final RestTemplate restTemplateLoopback;
    private final WebClient webClient;

    Clients(SslBundles sslBundles,
            SslContextFactory sslContextFactory,
            RestClient.Builder restClientBuilder,
            RestTemplateBuilder restTemplateBuilder,
            WebClient.Builder webClientBuilder) throws KeyManagementException, NoSuchAlgorithmException, SSLException {
        SslBundle testBundle = sslBundles.getBundle("client");
        SslBundle caCerts = sslBundles.getBundle("cacerts");

        SSLContext sslContext = sslContextFactory.build(testBundle, caCerts);
        HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.restTemplate = restTemplateBuilder.requestFactory(() -> requestFactory).build();

        ClientHttpRequestFactory loopbackClientHttpRequestFactory = getClientHttpRequestFactory(testBundle);
        this.restClientLoopback = restClientBuilder.requestFactory(loopbackClientHttpRequestFactory).build();
        this.restTemplateLoopback = restTemplateBuilder.requestFactory(() -> loopbackClientHttpRequestFactory).build();

        SslContext reactorSslContext = sslContextFactory.buildReactorSslContext(testBundle, caCerts);
        reactor.netty.http.client.HttpClient reactorHttpClient = reactor.netty.http.client.HttpClient
            .create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(reactorSslContext));
        this.webClient = webClientBuilder.clientConnector(new ReactorClientHttpConnector(reactorHttpClient)).build();
    }

    private static class LoopbackIPHostnameVerifier implements HostnameVerifier {

        private final DefaultHostnameVerifier defaultHostnameVerifier = new DefaultHostnameVerifier();

        @Override
        public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
            if ("127.0.0.1".equals(hostname)) {
                return true;
            }
            return defaultHostnameVerifier.verify(hostname, sslSession);
        }
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory(SslBundle sslBundle) throws NoSuchAlgorithmException, KeyManagementException {
        // Build request factory with SSLContext from SslBundle but with a custom LoopbackIPHostnameVerifier
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
            sslBundle
                .getManagers()
                .createSslContext(sslBundle.getProtocol()),
            new LoopbackIPHostnameVerifier());

        HttpClientConnectionManager httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder
            .create()
            .setSSLSocketFactory(sslConnectionSocketFactory)
            .build();
        CloseableHttpClient closeableHttpClient = HttpClients.custom()
            .setConnectionManager(httpClientConnectionManager)
            .evictExpiredConnections()
            .build();
        return new HttpComponentsClientHttpRequestFactory(closeableHttpClient);
    }

    public RestClient getRestClientLoopback() {
        return restClientLoopback;
    }

    public RestTemplate getRestTemplateLoopback() {
        return restTemplateLoopback;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public WebClient getWebClient() {
        return webClient;
    }
}


