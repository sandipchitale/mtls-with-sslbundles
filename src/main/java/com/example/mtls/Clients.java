package com.example.mtls;

import io.netty.handler.ssl.SslContext;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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
    private final RestTemplate restTemplate;
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

        SslContext reactorSslContext = sslContextFactory.buildReactorSslContext(testBundle, caCerts);
        reactor.netty.http.client.HttpClient reactorHttpClient = reactor.netty.http.client.HttpClient
            .create()
            .secure(sslContextSpec -> sslContextSpec.sslContext(reactorSslContext));
        this.webClient = webClientBuilder.clientConnector(new ReactorClientHttpConnector(reactorHttpClient)).build();
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
