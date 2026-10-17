package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.request.AdventureRequest;
import com.trisha.bff.model.dto.request.MoveRegionRequest;
import com.trisha.bff.model.dto.response.AdventureDetailResponse;
import com.trisha.bff.model.dto.response.AdventureResponse;
import com.trisha.bff.model.dto.response.FeedAdventureResponse;
import com.trisha.bff.model.dto.response.LeaveAdventureResponse;
import com.trisha.bff.model.dto.response.PathResponse;
import com.trisha.bff.model.dto.response.MediaResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestra operacoes de aventura sobre o servico APP.
 *
 * O cache "aventura" guarda a aventura por id; o cache "aventuras-usuario"
 * guarda a lista por usuario. Escritas (status, participante, delete)
 * invalidam ambos, pois alteram tanto a aventura quanto as listas que a
 * contem — invalidar a lista inteira do usuario e mais simples e seguro do
 * que tentar atualizar entradas individuais.
 *
 * As chaves das leituras incluem o observador: o APP filtra por visibilidade,
 * entao cachear so por id vazaria dado de um usuario autorizado para outro.
 * Por isso as escritas invalidam com allEntries (nao da para prever as chaves
 * por-usuario existentes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdventureBffService {

    /**
     * Pagina usada na agregacao da tela de aventura. A tela mostra os primeiros
     * caminhos e midias da aventura; o front pagina o resto via os endpoints
     * dedicados de caminho/midia. Mantemos um teto generoso para nao perder itens
     * na visao inicial sem trazer colecoes ilimitadas.
     */
    private static final Pageable FIRST_DETAIL_PAGE = PageRequest.of(0, 50);

    private final AppClient appClient;

    public AdventureResponse create(AdventureRequest request) {
        log.info("BFF: criando aventura para o destino {}", request.destination());
        return appClient.createAdventure(request);
    }

    @Cacheable(cacheNames = "aventura", key = "#observerId + '-' + #id")
    public AdventureResponse getById(String observerId, String id) {
        return appClient.getAdventure(id);
    }

    /**
     * Feed do app: aventuras do usuario + as visiveis de quem ele segue (a
     * selecao e do APP, pelo Bearer), enriquecidas com nome/codigo do autor.
     * Sem cache: o resultado e por usuario e o feed pede frescor.
     */
    public PageResponse<FeedAdventureResponse> getFeed(Pageable pageable) {
        PageResponse<AdventureResponse> page = appClient.getFeed(pageable);
        if (page.content().isEmpty()) {
            return new PageResponse<>(List.of(), page.number(), page.size(),
                    page.totalElements(), page.totalPages());
        }

        Map<String, UserSummaryResponse> users = appClient
                .getUserSummaries(page.content().stream().map(AdventureResponse::userId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UserSummaryResponse::id, Function.identity()));

        List<FeedAdventureResponse> items = page.content().stream()
                .map(adventure -> {
                    UserSummaryResponse user = users.get(adventure.userId());
                    return new FeedAdventureResponse(
                            adventure.id(),
                            adventure.userId(),
                            user == null ? null : user.name(),
                            user == null ? null : user.userCode(),
                            adventure.regionId(),
                            adventure.destination(),
                            adventure.status(),
                            adventure.visibility(),
                            adventure.createdAt(),
                            adventure.participantsCount(),
                            adventure.durationHours());
                })
                .toList();

        return new PageResponse<>(items, page.number(), page.size(), page.totalElements(), page.totalPages());
    }

    /**
     * Tela de aventura: junta a aventura, seus caminhos e suas midias numa unica
     * resposta. O app faz UMA chamada em vez de tres sequenciais. Tudo vem do
     * APP (que concentra esses dados), entao continua sendo uma agregacao barata.
     */
    @Cacheable(cacheNames = "aventura-detalhe", key = "#observerId + '-' + #id")
    public AdventureDetailResponse getDetail(String observerId, String id) {
        log.info("BFF: montando tela de aventura {}", id);
        AdventureResponse adventure = appClient.getAdventure(id);
        List<PathResponse> paths = appClient.getPathsByAdventure(id, FIRST_DETAIL_PAGE).content();
        List<MediaResponse> media = appClient.getMediaByAdventure(id, FIRST_DETAIL_PAGE).content();
        return new AdventureDetailResponse(adventure, paths, media);
    }

    @Cacheable(cacheNames = "aventuras-usuario",
            key = "#observerId + '-' + #userId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<AdventureResponse> getByUser(String observerId, String userId, Pageable pageable) {
        return appClient.getAdventuresByUser(userId, pageable);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "aventura", allEntries = true),
            @CacheEvict(cacheNames = "aventura-detalhe", allEntries = true),
            @CacheEvict(cacheNames = "aventuras-usuario", allEntries = true)
    })
    public AdventureResponse updateStatus(String id, String status) {
        log.info("BFF: atualizando status da aventura {} para {}", id, status);
        return appClient.updateAdventureStatus(id, status);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "aventura", allEntries = true),
            @CacheEvict(cacheNames = "aventura-detalhe", allEntries = true),
            @CacheEvict(cacheNames = "aventuras-usuario", allEntries = true)
    })
    public AdventureResponse moveRegion(String id, MoveRegionRequest request) {
        log.info("BFF: movendo aventura {} de pasta", id);
        return appClient.moveAdventureRegion(id, request);
    }

    @CacheEvict(cacheNames = "aventura", allEntries = true)
    public void addParticipant(String adventureId, String userId) {
        log.info("BFF: adicionando participante {} a aventura {}", userId, adventureId);
        appClient.addParticipant(adventureId, userId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "aventura", allEntries = true),
            @CacheEvict(cacheNames = "aventura-detalhe", allEntries = true),
            @CacheEvict(cacheNames = "aventuras-usuario", allEntries = true)
    })
    public LeaveAdventureResponse leave(String adventureId, boolean keepData) {
        log.info("BFF: saindo da aventura {} (manterDados={})", adventureId, keepData);
        return appClient.leaveAdventure(adventureId, keepData);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "aventura", allEntries = true),
            @CacheEvict(cacheNames = "aventura-detalhe", allEntries = true),
            @CacheEvict(cacheNames = "aventuras-usuario", allEntries = true)
    })
    public LeaveAdventureResponse kick(String adventureId, String userId) {
        log.info("BFF: removendo participante {} da aventura {}", userId, adventureId);
        return appClient.kickParticipant(adventureId, userId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "aventura", allEntries = true),
            @CacheEvict(cacheNames = "aventura-detalhe", allEntries = true),
            @CacheEvict(cacheNames = "aventuras-usuario", allEntries = true)
    })
    public void delete(String id) {
        log.info("BFF: deletando aventura {}", id);
        appClient.deleteAdventure(id);
    }
}
