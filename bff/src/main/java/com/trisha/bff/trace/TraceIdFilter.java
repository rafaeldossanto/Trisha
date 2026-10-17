package com.trisha.bff.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Borda do sistema: gera o traceId da requisicao (ou reaproveita o header
 * {@code X-Trace-Id} se ja vier de um cliente) e o coloca no MDC. A partir daqui
 * o id aparece nos logs e e propagado aos servicos downstream pelo RestClient.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TraceContext.HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceContext.generate();
        }
        MDC.put(TraceContext.MDC_KEY, traceId);
        response.setHeader(TraceContext.HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.MDC_KEY);
        }
    }
}
