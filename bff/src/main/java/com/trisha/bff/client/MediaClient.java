package com.trisha.bff.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class MediaClient {

    private final RestClient midiaRestClient;

    public MediaClient(@Qualifier("midiaRestClient") RestClient midiaRestClient) {
        this.midiaRestClient = midiaRestClient;
    }
}
