package com.trisha.bff.config;

import com.trisha.bff.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Propaga o traceId (header {@code X-Trace-Id}) do MDC para os servicos
 * downstream, para a mesma requisicao ser correlacionavel em todos os logs.
 */
@Component
public class TraceIdPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String traceId = MDC.get(TraceContext.MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            request.getHeaders().set(TraceContext.HEADER, traceId);
        }
        return execution.execute(request, body);
    }
}
