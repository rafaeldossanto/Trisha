package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.request.RegionRequest;
import com.trisha.bff.model.dto.response.AdventureResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.RegionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Orquestra as regioes (pastas) sobre o servico APP. Sem cache: regiao agora e
 * dado por usuario e mutavel (CRUD), entao seria mais risco de stale do que ganho.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegionBffService {

    private final AppClient appClient;

    public RegionResponse create(RegionRequest request) {
        log.info("BFF: criando regiao {}", request.name());
        return appClient.createRegion(request);
    }

    public PageResponse<RegionResponse> mine(Pageable pageable) {
        return appClient.getMyRegions(pageable);
    }

    public PageResponse<RegionResponse> discover(Pageable pageable) {
        return appClient.discoverRegions(pageable);
    }

    public RegionResponse getById(String id) {
        return appClient.getRegion(id);
    }

    public PageResponse<AdventureResponse> adventures(String id, Pageable pageable) {
        return appClient.getAdventuresByRegion(id, pageable);
    }

    public RegionResponse update(String id, RegionRequest request) {
        return appClient.updateRegion(id, request);
    }

    public void delete(String id) {
        log.info("BFF: apagando regiao {}", id);
        appClient.deleteRegion(id);
    }
}
