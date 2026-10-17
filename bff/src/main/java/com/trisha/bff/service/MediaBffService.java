package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.request.MediaRequest;
import com.trisha.bff.model.dto.response.MediaResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Orquestra os metadados de midia sobre o servico APP. O BFF lida apenas com
 * metadados (incluindo a URL do binario): o upload do binario em si vai direto
 * do front para o servico de Midia, sem passar pelo BFF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaBffService {

    private final AppClient appClient;

    @Caching(evict = {
            @CacheEvict(cacheNames = "midias-aventura", allEntries = true),
            @CacheEvict(cacheNames = "midias-caminho", allEntries = true)
    })
    public MediaResponse save(MediaRequest request) {
        log.info("BFF: salvando metadados de midia na aventura {}", request.adventureId());
        return appClient.saveMedia(request);
    }

    // Chave inclui o observador: o APP nega por visibilidade da aventura, e um
    // cache so por id serviria a resposta de um usuario autorizado a outro nao.
    @Cacheable(cacheNames = "midias-aventura",
            key = "#observerId + '-' + #adventureId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<MediaResponse> getByAdventure(String observerId, String adventureId, Pageable pageable) {
        return appClient.getMediaByAdventure(adventureId, pageable);
    }

    @Cacheable(cacheNames = "midias-caminho",
            key = "#observerId + '-' + #pathId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<MediaResponse> getByPath(String observerId, String pathId, Pageable pageable) {
        return appClient.getMediaByPath(pathId, pageable);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "midias-aventura", allEntries = true),
            @CacheEvict(cacheNames = "midias-caminho", allEntries = true)
    })
    public void delete(String id) {
        log.info("BFF: deletando metadados de midia {}", id);
        appClient.deleteMedia(id);
    }
}
