package com.trisha.bff.service;

import com.trisha.bff.auth.AuthenticatedUser;
import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.request.FriendshipRequest;
import com.trisha.bff.model.dto.response.FriendshipResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Orquestra operacoes de amizade sobre o servico APP. O ator vem do token
 * (propagado downstream); as listagens sao do proprio usuario autenticado.
 *
 * Escritas invalidam as listas inteiras (allEntries) — simples e seguro para um
 * dominio de baixo volume de escrita.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipBffService {

    private final AppClient appClient;

    @Caching(evict = {
            @CacheEvict(cacheNames = "amigos", allEntries = true),
            @CacheEvict(cacheNames = "amizades-pendentes", allEntries = true)
    })
    public FriendshipResponse request(FriendshipRequest request) {
        log.info("BFF: solicitacao de amizade para {}", request.receiverCode());
        return appClient.requestFriendship(request);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "amigos", allEntries = true),
            @CacheEvict(cacheNames = "amizades-pendentes", allEntries = true)
    })
    public FriendshipResponse respond(String id, String status) {
        log.info("BFF: respondendo amizade {} com status {}", id, status);
        return appClient.respondFriendship(id, status);
    }

    @Cacheable(cacheNames = "amizades-pendentes",
            key = "#user.id + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<FriendshipResponse> getPending(AuthenticatedUser user, Pageable pageable) {
        return appClient.getPending(pageable);
    }

    @Cacheable(cacheNames = "amigos",
            key = "#user.id + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<FriendshipResponse> getFriends(AuthenticatedUser user, Pageable pageable) {
        return appClient.getFriends(pageable);
    }
}
