package com.app.APP.trace;

import org.slf4j.MDC;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Correlacao de requisicoes (traceId). Inteiro de 6 digitos gerado na borda
 * (BFF) e propagado via header {@code X-Trace-Id}; este servico o le, coloca no
 * MDC (logs) e grava nas linhas que cria.
 */
public final class TraceContext {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private TraceContext() {
    }

    /** Current traceId for the request (from MDC), or null outside a request. */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** Generates a traceId: random 6-digit integer (100000-999999). */
    public static String generate() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
    }
}
