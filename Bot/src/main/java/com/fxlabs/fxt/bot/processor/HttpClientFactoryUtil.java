package com.fxlabs.fxt.bot.processor;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class HttpClientFactoryUtil {

    private static HttpComponentsClientHttpRequestFactory FACTORY = null;

    public static final HttpComponentsClientHttpRequestFactory getInstance_() {

        if (FACTORY != null) {
            return FACTORY;
        }

        CloseableHttpClient httpClient
                = HttpClients.custom()
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        FACTORY = new HttpComponentsClientHttpRequestFactory();

        FACTORY.setHttpClient(httpClient);

        int timeout = 15000;
        FACTORY.setConnectTimeout(timeout);
        FACTORY.setConnectionRequestTimeout(timeout);
        FACTORY.setReadTimeout(timeout);

        return FACTORY;
    }


    public static void setRequestFactory(RestTemplate restTemplate) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        if (FACTORY == null) {
            FACTORY = new HttpComponentsClientHttpRequestFactory();
            int timeout = 15000;
            FACTORY.setConnectTimeout(timeout);
            FACTORY.setConnectionRequestTimeout(timeout);
            FACTORY.setReadTimeout(timeout);
        }
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .build();


            FACTORY.setHttpClient(httpClient);
            
        restTemplate.setRequestFactory(FACTORY);
    }
}
