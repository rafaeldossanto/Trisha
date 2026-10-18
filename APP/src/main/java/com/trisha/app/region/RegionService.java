package com.trisha.app.region;

import com.trisha.app.adventure.AdventureService;
import com.trisha.app.adventure.dto.AdventureResponse;
import com.trisha.app.region.dto.RegionRequest;
import com.trisha.app.region.dto.RegionResponse;
import com.trisha.app.shared.util.OwnershipValidator;
import com.trisha.app.social.FriendshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionService {

    private final RegionRepository regionRepository;
    private final FriendshipService friendshipService;
    private final AdventureService adventureService;

    @Transactional
    public RegionResponse create(String userId, RegionRequest request) {
        log.info("Creating region '{}' for user {}", request.name(), userId);
        return RegionMapper.toResponse(regionRepository.save(RegionMapper.toEntity(request, userId)));
    }

    @Transactional(readOnly = true)
    public Page<RegionResponse> listMine(String userId, Pageable pageable) {
        return regionRepository.findByUserId(userId, pageable).map(RegionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RegionResponse getById(String userId, String id) {
        Region region = find(id);
        validateAccess(region, userId);
        return RegionMapper.toResponse(region);
    }

    @Transactional
    public RegionResponse update(String userId, String id, RegionRequest request) {
        Region region = findOwner(id, userId);
        RegionMapper.apply(region, request);
        return RegionMapper.toResponse(regionRepository.save(region));
    }

    @Transactional
    public void delete(String userId, String id) {
        Region region = findOwner(id, userId);
        adventureService.unlinkRegion(id);
        regionRepository.delete(region);
        log.info("Region {} deleted; adventures unlinked", id);
    }

    @Transactional(readOnly = true)
    public Page<RegionResponse> discover(String userId, Pageable pageable) {
        List<String> friends = friendshipService.friendIds(userId);
        List<String> filter = friends.isEmpty() ? List.of("__sem_amigos__") : friends;
        return regionRepository.findDiscoverable(userId, filter, pageable).map(RegionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AdventureResponse> getAdventures(String userId, String regionId, Pageable pageable) {
        Region region = find(regionId);
        validateAccess(region, userId);
        return adventureService.listVisibleInRegion(regionId, userId, pageable);
    }

    private void validateAccess(Region region, String userId) {
        if (userId.equals(region.getUserId())) {
            return;
        }
        boolean allowed = switch (region.getVisibility()) {
            case PUBLICA -> true;
            case AMIGOS -> friendshipService.areFriends(userId, region.getUserId());
            case PRIVADA -> false;
        };
        if (!allowed) {
            throw new IllegalArgumentException("Regiao nao encontrada ou sem acesso");
        }
    }

    private Region find(String id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regiao nao encontrada"));
    }

    private Region findOwner(String id, String userId) {
        Region region = find(id);
        OwnershipValidator.requireOwner(userId, region.getUserId(), "Voce nao e o dono desta regiao");
        return region;
    }
}
