package com.trisha.bff.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ServicosProperties.class)
public class RestClientConfig {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final ServicosProperties services;
    private final BearerPropagationInterceptor bearerInterceptor;
    private final TraceIdPropagationInterceptor traceInterceptor;

    public RestClientConfig(ServicosProperties services,
                            BearerPropagationInterceptor bearerInterceptor,
                            TraceIdPropagationInterceptor traceInterceptor) {
        this.services = services;
        this.bearerInterceptor = bearerInterceptor;
        this.traceInterceptor = traceInterceptor;
    }

    @Bean
    public RestClient cadastroRestClient() {
        return build(services.getCadastro());
    }

    @Bean
    public RestClient appRestClient() {
        return build(services.getApp());
    }

    @Bean
    public RestClient localizacaoRestClient() {
        return build(services.getLocalizacao());
    }

    @Bean
    public RestClient midiaRestClient() {
        return build(services.getMidia());
    }

    private RestClient build(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .requestInterceptor(bearerInterceptor)
                .requestInterceptor(traceInterceptor)
                .build();
    }

    /**
     * java.net.http.HttpClient em vez de HttpURLConnection: o Simple factory
     * NAO suporta o metodo PATCH ("Invalid HTTP method: PATCH"), que os
     * downstreams usam (finalizar sessao/caminho, visibilidade, status).
     */
    private JdkClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
