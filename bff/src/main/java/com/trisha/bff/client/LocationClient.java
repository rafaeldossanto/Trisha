package com.trisha.bff.client;

import com.trisha.bff.exception.ClientFallbacks;
import com.trisha.bff.exception.ServiceUnavailableException;
import com.trisha.bff.model.dto.request.GpsPointRequest;
import com.trisha.bff.model.dto.request.SessionRequest;
import com.trisha.bff.model.dto.response.GpsPointResponse;
import com.trisha.bff.model.dto.response.LiveSessionResponse;
import com.trisha.bff.model.dto.response.SessionResponse;
import com.trisha.bff.model.dto.response.TrailPointsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;


@Component
@Slf4j
public class LocationClient {

    private static final ParameterizedTypeReference<List<GpsPointResponse>> LIST_POINT = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<TrailPointsResponse>> LIST_TRAIL = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<LiveSessionResponse>> LIST_LIVE = new ParameterizedTypeReference<>() {};

    private final RestClient localizacaoRestClient;

    public LocationClient(@Qualifier("localizacaoRestClient") RestClient localizacaoRestClient) {
        this.localizacaoRestClient = localizacaoRestClient;
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackStartSession")
    @Retry(name = "localizacao")
    public SessionResponse startSession(SessionRequest request) {
        return localizacaoRestClient.post().uri("/localizacao/sessao").body(request)
                .retrieve().body(SessionResponse.class);
    }

    public SessionResponse fallbackStartSession(SessionRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao iniciar sessao de localizacao - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackRegisterPoint")
    @Retry(name = "localizacao")
    public GpsPointResponse registerPoint(GpsPointRequest request) {
        return localizacaoRestClient.post().uri("/localizacao/ponto").body(request)
                .retrieve().body(GpsPointResponse.class);
    }

    public GpsPointResponse fallbackRegisterPoint(GpsPointRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao registrar ponto GPS - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackFinishSession")
    @Retry(name = "localizacao")
    public SessionResponse finishSession(String id) {
        return localizacaoRestClient.patch().uri("/localizacao/sessao/{id}/finalizar", id)
                .retrieve().body(SessionResponse.class);
    }

    public SessionResponse fallbackFinishSession(String id, Throwable t) {
        log.error("Circuit breaker: falha ao finalizar sessao {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackCancelSession")
    @Retry(name = "localizacao")
    public SessionResponse cancelSession(String id) {
        return localizacaoRestClient.patch().uri("/localizacao/sessao/{id}/cancelar", id)
                .retrieve().body(SessionResponse.class);
    }

    public SessionResponse fallbackCancelSession(String id, Throwable t) {
        log.error("Circuit breaker: falha ao cancelar sessao {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackUpdateVisibility")
    @Retry(name = "localizacao")
    public SessionResponse updateSessionVisibility(String id, String visibility) {
        return localizacaoRestClient.patch()
                .uri(b -> b.path("/localizacao/sessao/{id}/visibilidade")
                        .queryParam("visibilidade", visibility).build(id))
                .retrieve().body(SessionResponse.class);
    }

    public SessionResponse fallbackUpdateVisibility(String id, String visibility, Throwable t) {
        log.error("Circuit breaker: falha ao alterar visibilidade da sessao {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackGetSession")
    @Retry(name = "localizacao")
    public SessionResponse getSession(String id) {
        return localizacaoRestClient.get().uri("/localizacao/sessao/{id}", id)
                .retrieve().body(SessionResponse.class);
    }

    public SessionResponse fallbackGetSession(String id, Throwable t) {
        log.error("Circuit breaker: falha ao buscar sessao {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    /** Checagem de acompanhamento ao vivo — falha FECHADA (erro nunca vira "pode ver"). */
    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackCanWatchSession")
    @Retry(name = "localizacao")
    public boolean canWatchSession(String id) {
        return Boolean.TRUE.equals(localizacaoRestClient.get()
                .uri("/localizacao/sessao/{id}/acesso", id)
                .retrieve().body(Boolean.class));
    }

    public boolean fallbackCanWatchSession(String id, Throwable t) {
        log.error("Circuit breaker: falha na checagem de acesso da sessao {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackGetSessionByPath")
    @Retry(name = "localizacao")
    public SessionResponse getSessionByPath(String pathId) {
        return localizacaoRestClient.get().uri("/localizacao/sessao/caminho/{caminhoId}", pathId)
                .retrieve().body(SessionResponse.class);
    }

    public SessionResponse fallbackGetSessionByPath(String pathId, Throwable t) {
        log.error("Circuit breaker: falha ao buscar sessao do caminho {} - {}", pathId, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico de localizacao temporariamente indisponivel.");
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackGetPointsBySession")
    @Retry(name = "localizacao")
    public List<GpsPointResponse> getPointsBySession(String sessionId) {
        return localizacaoRestClient.get().uri("/localizacao/pontos/sessao/{sessaoId}", sessionId)
                .retrieve().body(LIST_POINT);
    }

    public List<GpsPointResponse> fallbackGetPointsBySession(String sessionId, Throwable t) {
        log.warn("Circuit breaker: falha ao listar pontos da sessao {} - {}", sessionId, t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackGetPointsByPath")
    @Retry(name = "localizacao")
    public List<GpsPointResponse> getPointsByPath(String pathId) {
        return localizacaoRestClient.get().uri("/localizacao/pontos/caminho/{caminhoId}", pathId)
                .retrieve().body(LIST_POINT);
    }

    public List<GpsPointResponse> fallbackGetPointsByPath(String pathId, Throwable t) {
        log.warn("Circuit breaker: falha ao listar pontos do caminho {} - {}", pathId, t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackGetLiveSessions")
    @Retry(name = "localizacao")
    public List<LiveSessionResponse> getLiveSessions() {
        return localizacaoRestClient.get().uri("/localizacao/sessoes/ao-vivo")
                .retrieve().body(LIST_LIVE);
    }

    public List<LiveSessionResponse> fallbackGetLiveSessions(Throwable t) {
        log.warn("Circuit breaker: falha ao listar sessoes ao vivo - {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "localizacao", fallbackMethod = "fallbackGetPointsInBbox")
    @Retry(name = "localizacao")
    public List<TrailPointsResponse> getPointsInBbox(double minLat, double minLng, double maxLat, double maxLng,
                                                     int maxPointsPerPath) {
        return localizacaoRestClient.get()
                .uri(b -> b.path("/localizacao/pontos/bbox")
                        .queryParam("minLat", minLat)
                        .queryParam("minLng", minLng)
                        .queryParam("maxLat", maxLat)
                        .queryParam("maxLng", maxLng)
                        .queryParam("limitePorCaminho", maxPointsPerPath).build())
                .retrieve().body(LIST_TRAIL);
    }

    public List<TrailPointsResponse> fallbackGetPointsInBbox(double minLat, double minLng, double maxLat,
                                                             double maxLng, int maxPointsPerPath, Throwable t) {
        log.warn("Circuit breaker: falha ao buscar trilhas na bbox - {}", t.getMessage());
        return Collections.emptyList();
    }
}
