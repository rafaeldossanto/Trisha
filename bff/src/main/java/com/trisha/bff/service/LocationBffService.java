package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.client.LocationClient;
import com.trisha.bff.model.dto.request.GpsPointRequest;
import com.trisha.bff.model.dto.request.SessionRequest;
import com.trisha.bff.model.dto.response.GpsPointResponse;
import com.trisha.bff.model.dto.response.LiveSessionResponse;
import com.trisha.bff.model.dto.response.SessionResponse;
import com.trisha.bff.model.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestra sessoes de rastreamento e pontos GPS sobre o servico de Localizacao.
 *
 * Pontos GPS sao gravados em alta frequencia durante uma trilha em andamento,
 * entao registerPoint NAO cacheia (seria contraproducente) e invalida as
 * listas de pontos. As leituras de pontos so fazem sentido cachear apos a
 * sessao finalizar — para o MVP, o TTL curto ja cobre isso.
 *
 * As leituras de pontos sao GATEADAS pela visibilidade (aventura no APP; ou,
 * em sessao ao vivo, a visibilidade da sessao no loc) — e por isso as chaves
 * de cache incluem o observador: um cache por id vazaria o resultado de um
 * usuario autorizado para um nao autorizado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationBffService {

    private static final String STATUS_IN_PROGRESS = "EM_ANDAMENTO";

    private final LocationClient locationClient;
    private final AppClient appClient;

    public SessionResponse startSession(SessionRequest request) {
        log.info("BFF: iniciando sessao para caminho {}", request.pathId());
        return locationClient.startSession(request);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "pontos-sessao", allEntries = true),
            @CacheEvict(cacheNames = "pontos-caminho-gps", allEntries = true)
    })
    public GpsPointResponse registerPoint(GpsPointRequest request) {
        return locationClient.registerPoint(request);
    }

    public SessionResponse finishSession(String id) {
        log.info("BFF: finalizando sessao {}", id);
        return locationClient.finishSession(id);
    }

    public SessionResponse cancelSession(String id) {
        log.info("BFF: cancelando sessao {}", id);
        return locationClient.cancelSession(id);
    }

    /**
     * Troca a visibilidade do acompanhamento ao vivo. Invalida o cache de
     * sessao por caminho — senao o app leria a visibilidade antiga ate o TTL.
     */
    @CacheEvict(cacheNames = "sessao-caminho", allEntries = true)
    public SessionResponse updateVisibility(String id, String visibility) {
        log.info("BFF: alterando visibilidade da sessao {} para {}", id, visibility);
        return locationClient.updateSessionVisibility(id, visibility);
    }

    @Cacheable(cacheNames = "sessao-caminho", key = "#pathId")
    public SessionResponse getSessionByPath(String pathId) {
        return locationClient.getSessionByPath(pathId);
    }

    /**
     * Sessoes ao vivo que o usuario pode acompanhar, enriquecidas com o
     * nome/codigo do trilheiro (o loc so conhece ids). Sem cache: e tempo real.
     */
    public List<LiveSessionResponse> getLiveSessions() {
        List<LiveSessionResponse> sessions = locationClient.getLiveSessions();
        if (sessions.isEmpty()) {
            return sessions;
        }

        Map<String, UserSummaryResponse> users = summariesById(
                sessions.stream().map(LiveSessionResponse::userId).distinct().toList());

        return sessions.stream()
                .map(session -> new LiveSessionResponse(
                        session.sessionId(),
                        session.pathId(),
                        session.userId(),
                        nameOf(users, session.userId()),
                        codeOf(users, session.userId()),
                        session.visibility(),
                        session.startedAt(),
                        session.latitude(),
                        session.longitude()))
                .toList();
    }

    /**
     * Trajeto de uma sessao: liberado se a aventura do caminho e visivel ao
     * observador OU, com a sessao em andamento, se a visibilidade dela permite
     * (catch-up do acompanhamento ao vivo).
     */
    @Cacheable(cacheNames = "pontos-sessao", key = "#observerId + '-' + #sessionId")
    public List<GpsPointResponse> getPointsBySession(String observerId, String sessionId) {
        SessionResponse session = locationClient.getSession(sessionId);
        boolean allowed = (STATUS_IN_PROGRESS.equals(session.status()) && locationClient.canWatchSession(sessionId))
                || appClient.canViewPath(session.pathId());
        if (!allowed) {
            throw new IllegalArgumentException("Sessao nao encontrada ou sem acesso");
        }
        return locationClient.getPointsBySession(sessionId);
    }

    @Cacheable(cacheNames = "pontos-caminho-gps", key = "#observerId + '-' + #pathId")
    public List<GpsPointResponse> getPointsByPath(String observerId, String pathId) {
        if (!appClient.canViewPath(pathId)) {
            throw new IllegalArgumentException("Caminho nao encontrado ou sem acesso");
        }
        return locationClient.getPointsByPath(pathId);
    }

    private Map<String, UserSummaryResponse> summariesById(List<String> ids) {
        return appClient.getUserSummaries(ids).stream()
                .collect(Collectors.toMap(UserSummaryResponse::id, Function.identity()));
    }

    private String nameOf(Map<String, UserSummaryResponse> users, String userId) {
        UserSummaryResponse user = users.get(userId);
        return user == null ? null : user.name();
    }

    private String codeOf(Map<String, UserSummaryResponse> users, String userId) {
        UserSummaryResponse user = users.get(userId);
        return user == null ? null : user.userCode();
    }
}
