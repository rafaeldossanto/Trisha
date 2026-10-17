package com.trisha.bff.client;

import com.trisha.bff.exception.ClientFallbacks;
import com.trisha.bff.exception.ServiceUnavailableException;
import com.trisha.bff.model.dto.request.FriendshipRequest;
import com.trisha.bff.model.dto.request.AdventureRequest;
import com.trisha.bff.model.dto.request.PathRequest;
import com.trisha.bff.model.dto.request.EvidenceRequest;
import com.trisha.bff.model.dto.request.MediaRequest;
import com.trisha.bff.model.dto.request.MoveRegionRequest;
import com.trisha.bff.model.dto.request.PointOfInterestRequest;
import com.trisha.bff.model.dto.request.RegionRequest;
import com.trisha.bff.model.dto.request.FollowRequest;
import com.trisha.bff.model.dto.response.FriendshipResponse;
import com.trisha.bff.model.dto.response.AdventureResponse;
import com.trisha.bff.model.dto.response.CountersResponse;
import com.trisha.bff.model.dto.response.LeaveAdventureResponse;
import com.trisha.bff.model.dto.response.FollowStatusResponse;
import com.trisha.bff.model.dto.response.PathDiscoveryResponse;
import com.trisha.bff.model.dto.response.PathResponse;
import com.trisha.bff.model.dto.response.EvidenceResponse;
import com.trisha.bff.model.dto.response.MediaResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.PointOfInterestResponse;
import com.trisha.bff.model.dto.response.PointStatusResponse;
import com.trisha.bff.model.dto.response.RegionResponse;
import com.trisha.bff.model.dto.response.PublicUserResponse;
import com.trisha.bff.model.dto.response.UserSummaryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class AppClient {

    private static final ParameterizedTypeReference<PageResponse<AdventureResponse>> PAGE_ADVENTURE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<PathResponse>> PAGE_PATH = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<PathDiscoveryResponse>> LIST_PATH_DISCOVERY = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<UserSummaryResponse>> LIST_USER_SUMMARY = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<PointOfInterestResponse>> PAGE_POINT = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<PointStatusResponse>> LIST_POINT_STATUS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<MediaResponse>> PAGE_MEDIA = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<FriendshipResponse>> PAGE_FRIENDSHIP = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<PublicUserResponse>> LIST_PUBLIC_USER = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<PublicUserResponse>> PAGE_PUBLIC_USER = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<RegionResponse>> PAGE_REGION = new ParameterizedTypeReference<>() {};

    private final RestClient appRestClient;

    public AppClient(@Qualifier("appRestClient") RestClient appRestClient) {
        this.appRestClient = appRestClient;
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackCreateRegion")
    @Retry(name = "app")
    public RegionResponse createRegion(RegionRequest request) {
        return appRestClient.post().uri("/regiao").body(request)
                .retrieve().body(RegionResponse.class);
    }

    public RegionResponse fallbackCreateRegion(RegionRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao criar regiao - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetMyRegions")
    @Retry(name = "app")
    public PageResponse<RegionResponse> getMyRegions(Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/regiao")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize()).build())
                .retrieve().body(PAGE_REGION);
    }

    public PageResponse<RegionResponse> fallbackGetMyRegions(Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar regioes - {}", t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackDiscoverRegions")
    @Retry(name = "app")
    public PageResponse<RegionResponse> discoverRegions(Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/regiao/descobrir")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize()).build())
                .retrieve().body(PAGE_REGION);
    }

    public PageResponse<RegionResponse> fallbackDiscoverRegions(Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao descobrir regioes - {}", t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetRegion")
    @Retry(name = "app")
    public RegionResponse getRegion(String id) {
        return appRestClient.get().uri("/regiao/{id}", id)
                .retrieve().body(RegionResponse.class);
    }

    public RegionResponse fallbackGetRegion(String id, Throwable t) {
        log.error("Circuit breaker: falha ao buscar regiao {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetAdventuresByRegion")
    @Retry(name = "app")
    public PageResponse<AdventureResponse> getAdventuresByRegion(String regionId, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/regiao/{id}/aventuras")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize()).build(regionId))
                .retrieve().body(PAGE_ADVENTURE);
    }

    public PageResponse<AdventureResponse> fallbackGetAdventuresByRegion(String regionId, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar aventuras da regiao {} - {}", regionId, t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackUpdateRegion")
    @Retry(name = "app")
    public RegionResponse updateRegion(String id, RegionRequest request) {
        return appRestClient.put().uri("/regiao/{id}", id).body(request)
                .retrieve().body(RegionResponse.class);
    }

    public RegionResponse fallbackUpdateRegion(String id, RegionRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao atualizar regiao {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    public void deleteRegion(String id) {
        appRestClient.delete().uri("/regiao/{id}", id)
                .retrieve().toBodilessEntity();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackCreateAdventure")
    @Retry(name = "app")
    public AdventureResponse createAdventure(AdventureRequest request) {
        return appRestClient.post().uri("/aventura").body(request)
                .retrieve().body(AdventureResponse.class);
    }

    public AdventureResponse fallbackCreateAdventure(AdventureRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao criar aventura - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackMoveAdventureRegion")
    @Retry(name = "app")
    public AdventureResponse moveAdventureRegion(String id, MoveRegionRequest request) {
        return appRestClient.patch().uri("/aventura/{id}/regiao", id).body(request)
                .retrieve().body(AdventureResponse.class);
    }

    public AdventureResponse fallbackMoveAdventureRegion(String id, MoveRegionRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao mover aventura {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetAdventure")
    @Retry(name = "app")
    public AdventureResponse getAdventure(String id) {
        return appRestClient.get().uri("/aventura/{id}", id)
                .retrieve().body(AdventureResponse.class);
    }

    public AdventureResponse fallbackGetAdventure(String id, Throwable t) {
        log.error("Circuit breaker: falha ao buscar aventura {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetAdventuresByUser")
    @Retry(name = "app")
    public PageResponse<AdventureResponse> getAdventuresByUser(String userId, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/aventura/usuario/{usuarioId}")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build(userId))
                .retrieve().body(PAGE_ADVENTURE);
    }

    public PageResponse<AdventureResponse> fallbackGetAdventuresByUser(String userId, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar aventuras do usuario {} - {}", userId, t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackUpdateAdventureStatus")
    @Retry(name = "app")
    public AdventureResponse updateAdventureStatus(String id, String status) {
        return appRestClient.patch()
                .uri(b -> b.path("/aventura/{id}/status").queryParam("status", status).build(id))
                .retrieve().body(AdventureResponse.class);
    }

    public AdventureResponse fallbackUpdateAdventureStatus(String id, String status, Throwable t) {
        log.error("Circuit breaker: falha ao atualizar status da aventura {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    public void addParticipant(String adventureId, String userId) {
        appRestClient.post()
                .uri(b -> b.path("/aventura/{id}/participante").queryParam("usuarioId", userId).build(adventureId))
                .retrieve().toBodilessEntity();
    }

    /** O proprio usuario (via Bearer) sai da aventura; keepData preserva os registros. */
    public LeaveAdventureResponse leaveAdventure(String adventureId, boolean keepData) {
        return appRestClient.delete()
                .uri(b -> b.path("/aventura/{id}/participante").queryParam("manterDados", keepData).build(adventureId))
                .retrieve().body(LeaveAdventureResponse.class);
    }

    /** O dono remove um participante; os dados do removido sao sempre preservados. */
    public LeaveAdventureResponse kickParticipant(String adventureId, String userId) {
        return appRestClient.delete()
                .uri("/aventura/{id}/participante/{userId}", adventureId, userId)
                .retrieve().body(LeaveAdventureResponse.class);
    }

    public void deleteAdventure(String id) {
        appRestClient.delete().uri("/aventura/{id}", id)
                .retrieve().toBodilessEntity();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackStartPath")
    @Retry(name = "app")
    public PathResponse startPath(PathRequest request) {
        return appRestClient.post().uri("/caminho").body(request)
                .retrieve().body(PathResponse.class);
    }

    public PathResponse fallbackStartPath(PathRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao iniciar caminho - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackFinishPath")
    @Retry(name = "app")
    public PathResponse finishPath(String id, Double totalDistanceKm) {
        return appRestClient.patch()
                .uri(b -> b.path("/caminho/{id}/finalizar").queryParam("distanciaTotalKm", totalDistanceKm).build(id))
                .retrieve().body(PathResponse.class);
    }

    public PathResponse fallbackFinishPath(String id, Double totalDistanceKm, Throwable t) {
        log.error("Circuit breaker: falha ao finalizar caminho {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetPathsByAdventure")
    @Retry(name = "app")
    public PageResponse<PathResponse> getPathsByAdventure(String adventureId, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/caminho/aventura/{aventuraId}")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build(adventureId))
                .retrieve().body(PAGE_PATH);
    }

    public PageResponse<PathResponse> fallbackGetPathsByAdventure(String adventureId, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar caminhos da aventura {} - {}", adventureId, t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetPathsByUser")
    @Retry(name = "app")
    public PageResponse<PathResponse> getPathsByUser(String userId, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/caminho/usuario/{usuarioId}")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build(userId))
                .retrieve().body(PAGE_PATH);
    }

    public PageResponse<PathResponse> fallbackGetPathsByUser(String userId, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar caminhos do usuario {} - {}", userId, t.getMessage());
        return PageResponse.empty();
    }

    /** Checagem de acesso a um caminho — falha FECHADA (erro nunca vira "pode ver"). */
    @CircuitBreaker(name = "app", fallbackMethod = "fallbackCanViewPath")
    @Retry(name = "app")
    public boolean canViewPath(String pathId) {
        return Boolean.TRUE.equals(appRestClient.get().uri("/caminho/{id}/acesso", pathId)
                .retrieve().body(Boolean.class));
    }

    public boolean fallbackCanViewPath(String pathId, Throwable t) {
        log.error("Circuit breaker: falha na checagem de acesso do caminho {} - {}", pathId, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetUserSummaries")
    @Retry(name = "app")
    public List<UserSummaryResponse> getUserSummaries(List<String> ids) {
        return appRestClient.get()
                .uri(b -> b.path("/usuario/resumo")
                        .queryParam("ids", String.join(",", ids)).build())
                .retrieve().body(LIST_USER_SUMMARY);
    }

    public List<UserSummaryResponse> fallbackGetUserSummaries(List<String> ids, Throwable t) {
        log.warn("Circuit breaker: falha ao resolver nomes de usuarios - {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetFeed")
    @Retry(name = "app")
    public PageResponse<AdventureResponse> getFeed(Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/aventura/feed")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize()).build())
                .retrieve().body(PAGE_ADVENTURE);
    }

    public PageResponse<AdventureResponse> fallbackGetFeed(Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao montar o feed - {}", t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackDiscoverPaths")
    @Retry(name = "app")
    public List<PathDiscoveryResponse> discoverPaths(List<String> ids) {
        return appRestClient.get()
                .uri(b -> b.path("/caminho/descobrir")
                        .queryParam("ids", String.join(",", ids)).build())
                .retrieve().body(LIST_PATH_DISCOVERY);
    }

    public List<PathDiscoveryResponse> fallbackDiscoverPaths(List<String> ids, Throwable t) {
        log.warn("Circuit breaker: falha ao descobrir caminhos - {}", t.getMessage());
        return Collections.emptyList();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackCreatePoint")
    @Retry(name = "app")
    public PointOfInterestResponse createPoint(PointOfInterestRequest request) {
        return appRestClient.post().uri("/ponto-interesse").body(request)
                .retrieve().body(PointOfInterestResponse.class);
    }

    public PointOfInterestResponse fallbackCreatePoint(PointOfInterestRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao criar ponto de interesse - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetPoint")
    @Retry(name = "app")
    public PointOfInterestResponse getPoint(String id) {
        return appRestClient.get().uri("/ponto-interesse/{id}", id)
                .retrieve().body(PointOfInterestResponse.class);
    }

    public PointOfInterestResponse fallbackGetPoint(String id, Throwable t) {
        log.error("Circuit breaker: falha ao buscar ponto {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetPointsByPath")
    @Retry(name = "app")
    public PageResponse<PointOfInterestResponse> getPointsByPath(String pathId, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/ponto-interesse/caminho/{caminhoId}")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build(pathId))
                .retrieve().body(PAGE_POINT);
    }

    public PageResponse<PointOfInterestResponse> fallbackGetPointsByPath(String pathId, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar pontos do caminho {} - {}", pathId, t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackAddEvidence")
    @Retry(name = "app")
    public EvidenceResponse addEvidence(EvidenceRequest request) {
        return appRestClient.post().uri("/ponto-interesse/evidencia").body(request)
                .retrieve().body(EvidenceResponse.class);
    }

    public EvidenceResponse fallbackAddEvidence(EvidenceRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao adicionar evidencia - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackSetPointStatus")
    @Retry(name = "app")
    public PointStatusResponse setPointStatus(String id, String status) {
        return appRestClient.patch()
                .uri(b -> b.path("/ponto-interesse/{id}/status")
                        .queryParam("status", status).build(id))
                .retrieve().body(PointStatusResponse.class);
    }

    public PointStatusResponse fallbackSetPointStatus(String id, String status, Throwable t) {
        log.error("Circuit breaker: falha ao marcar status do ponto {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackClearPointStatus")
    @Retry(name = "app")
    public void clearPointStatus(String id) {
        appRestClient.delete().uri("/ponto-interesse/{id}/status", id)
                .retrieve().toBodilessEntity();
    }

    public void fallbackClearPointStatus(String id, Throwable t) {
        log.error("Circuit breaker: falha ao remover status do ponto {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackSetPointGoal")
    @Retry(name = "app")
    public PointStatusResponse setPointGoal(String id, boolean goal) {
        return appRestClient.patch()
                .uri(b -> b.path("/ponto-interesse/{id}/objetivo")
                        .queryParam("objetivo", goal).build(id))
                .retrieve().body(PointStatusResponse.class);
    }

    public PointStatusResponse fallbackSetPointGoal(String id, boolean goal, Throwable t) {
        log.error("Circuit breaker: falha ao marcar objetivo no ponto {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetPointStatuses")
    @Retry(name = "app")
    public List<PointStatusResponse> getPointStatuses(List<String> ids) {
        return appRestClient.get()
                .uri(b -> b.path("/ponto-interesse/status")
                        .queryParam("ids", String.join(",", ids)).build())
                .retrieve().body(LIST_POINT_STATUS);
    }

    public List<PointStatusResponse> fallbackGetPointStatuses(List<String> ids, Throwable t) {
        log.warn("Circuit breaker: falha ao buscar status dos pontos - {}", t.getMessage());
        return Collections.emptyList();
    }

    public MediaResponse saveMedia(MediaRequest request) {
        return appRestClient.post().uri("/midia").body(request)
                .retrieve().body(MediaResponse.class);
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetMediaByAdventure")
    @Retry(name = "app")
    public PageResponse<MediaResponse> getMediaByAdventure(String adventureId, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/midia/aventura/{aventuraId}")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build(adventureId))
                .retrieve().body(PAGE_MEDIA);
    }

    public PageResponse<MediaResponse> fallbackGetMediaByAdventure(String adventureId, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar midias da aventura {} - {}", adventureId, t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetMediaByPath")
    @Retry(name = "app")
    public PageResponse<MediaResponse> getMediaByPath(String pathId, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/midia/caminho/{caminhoId}")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build(pathId))
                .retrieve().body(PAGE_MEDIA);
    }

    public PageResponse<MediaResponse> fallbackGetMediaByPath(String pathId, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar midias do caminho {} - {}", pathId, t.getMessage());
        return PageResponse.empty();
    }

    public void deleteMedia(String id) {
        appRestClient.delete().uri("/midia/{id}", id)
                .retrieve().toBodilessEntity();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackRequestFriendship")
    @Retry(name = "app")
    public FriendshipResponse requestFriendship(FriendshipRequest request) {
        return appRestClient.post().uri("/amizade").body(request)
                .retrieve().body(FriendshipResponse.class);
    }

    public FriendshipResponse fallbackRequestFriendship(FriendshipRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao solicitar amizade - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackRespondFriendship")
    @Retry(name = "app")
    public FriendshipResponse respondFriendship(String id, String status) {
        return appRestClient.patch()
                .uri(b -> b.path("/amizade/{id}/responder").queryParam("status", status).build(id))
                .retrieve().body(FriendshipResponse.class);
    }

    public FriendshipResponse fallbackRespondFriendship(String id, String status, Throwable t) {
        log.error("Circuit breaker: falha ao responder amizade {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetPending")
    @Retry(name = "app")
    public PageResponse<FriendshipResponse> getPending(Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/amizade/pendentes")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve().body(PAGE_FRIENDSHIP);
    }

    public PageResponse<FriendshipResponse> fallbackGetPending(Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar amizades pendentes - {}", t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetFriends")
    @Retry(name = "app")
    public PageResponse<FriendshipResponse> getFriends(Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/amizade/amigos")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .retrieve().body(PAGE_FRIENDSHIP);
    }

    public PageResponse<FriendshipResponse> fallbackGetFriends(Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar amigos - {}", t.getMessage());
        return PageResponse.empty();
    }

    // ----------------------------- Seguidores ---------------------------

    public void follow(String code) {
        appRestClient.post().uri("/seguidor").body(new FollowRequest(code)).retrieve().toBodilessEntity();
    }

    public void unfollow(String code) {
        appRestClient.method(HttpMethod.DELETE).uri("/seguidor").body(new FollowRequest(code))
                .retrieve().toBodilessEntity();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetFollowers")
    @Retry(name = "app")
    public PageResponse<PublicUserResponse> getFollowers(String code, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/seguidor/seguidores")
                        .queryParam("codigo", code)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize()).build())
                .retrieve().body(PAGE_PUBLIC_USER);
    }

    public PageResponse<PublicUserResponse> fallbackGetFollowers(String code, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar seguidores de {} - {}", code, t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetFollowing")
    @Retry(name = "app")
    public PageResponse<PublicUserResponse> getFollowing(String code, Pageable pageable) {
        return appRestClient.get()
                .uri(b -> b.path("/seguidor/seguindo")
                        .queryParam("codigo", code)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize()).build())
                .retrieve().body(PAGE_PUBLIC_USER);
    }

    public PageResponse<PublicUserResponse> fallbackGetFollowing(String code, Pageable pageable, Throwable t) {
        log.warn("Circuit breaker: falha ao listar seguindo de {} - {}", code, t.getMessage());
        return PageResponse.empty();
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetCounters")
    @Retry(name = "app")
    public CountersResponse getCounters(String code) {
        return appRestClient.get()
                .uri(b -> b.path("/seguidor/contadores").queryParam("codigo", code).build())
                .retrieve().body(CountersResponse.class);
    }

    public CountersResponse fallbackGetCounters(String code, Throwable t) {
        log.warn("Circuit breaker: falha ao buscar contadores de {} - {}", code, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackGetFollowStatus")
    @Retry(name = "app")
    public FollowStatusResponse getFollowStatus(String code) {
        return appRestClient.get()
                .uri(b -> b.path("/seguidor/status").queryParam("codigo", code).build())
                .retrieve().body(FollowStatusResponse.class);
    }

    public FollowStatusResponse fallbackGetFollowStatus(String code, Throwable t) {
        log.warn("Circuit breaker: falha ao buscar status de seguimento de {} - {}", code, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    // --------------------------- Busca de usuario -----------------------

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackFindUserByCode")
    @Retry(name = "app")
    public PublicUserResponse findUserByCode(String userCode) {
        return appRestClient.get().uri("/usuario/codigo/{codigo}", userCode)
                .retrieve().body(PublicUserResponse.class);
    }

    public PublicUserResponse fallbackFindUserByCode(String userCode, Throwable t) {
        log.error("Circuit breaker: falha ao buscar usuario {} - {}", userCode, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackFindUserSummaryByCode")
    @Retry(name = "app")
    public UserSummaryResponse findUserSummaryByCode(String userCode) {
        return appRestClient.get()
                .uri(b -> b.path("/usuario/resumo-codigo").queryParam("codigo", userCode).build())
                .retrieve().body(UserSummaryResponse.class);
    }

    public UserSummaryResponse fallbackFindUserSummaryByCode(String userCode, Throwable t) {
        log.error("Circuit breaker: falha ao resumir usuario {} - {}", userCode, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "app", fallbackMethod = "fallbackAutocompleteUser")
    @Retry(name = "app")
    public List<PublicUserResponse> autocompleteUser(String term) {
        return appRestClient.get()
                .uri(b -> b.path("/usuario/busca").queryParam("termo", term).build())
                .retrieve().body(LIST_PUBLIC_USER);
    }

    public List<PublicUserResponse> fallbackAutocompleteUser(String term, Throwable t) {
        log.warn("Circuit breaker: falha ao autocompletar usuario com '{}' - {}", term, t.getMessage());
        return Collections.emptyList();
    }
}
