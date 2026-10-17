package com.trisha.bff.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Propaga o Bearer da requisicao atual para as chamadas downstream do BFF, para
 * que cada servico valide a mesma identidade (pass-through). Em rotas publicas
 * (sem token) a chamada segue sem o header.
 */
@Component
public class BearerPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            request.getHeaders().setBearerAuth(token.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    }
}
