package com.trisha.app.adventure;

import com.trisha.app.adventure.dto.AdventureRequest;
import com.trisha.app.adventure.dto.AdventureResponse;
import com.trisha.app.region.Region;
import com.trisha.app.region.RegionRepository;
import com.trisha.app.shared.util.OwnershipValidator;
import com.trisha.app.social.FollowerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdventureService {

    private final AdventureRepository adventureRepository;
    private final AdventureParticipantRepository participantRepository;
    private final RegionRepository regionRepository;
    private final FollowerRepository followerRepository;
    private final AdventureAccessService accessService;
    private final AdventureMetricsAssembler metricsAssembler;

    @Transactional
    public AdventureResponse create(String userId, AdventureRequest request) {
        log.info("Creating adventure for user: {}", userId);

        Region region = resolveRegion(userId, request.regionId());

        var adventure = saveWithOwner(AdventureMapper.toEntity(request, region, userId));

        log.info("Adventure created with id: {}", adventure.getId());
        return metricsAssembler.build(adventure);
    }

    /**
     * Aventura pessoal PRIVADA que recebe os registros de quem sai de uma
     * aventura em grupo (ver AdventureMembershipService).
     */
    @Transactional
    public Adventure createPersonalCopy(Adventure origin, String userId) {
        return saveWithOwner(AdventureMapper.toPersonalCopy(origin, userId));
    }

    /** Toda aventura nasce com o dono como participante. */
    private Adventure saveWithOwner(Adventure adventure) {
        Adventure saved = adventureRepository.save(adventure);
        participantRepository.save(ParticipantMapper.toEntity(saved, saved.getUserId()));
        return saved;
    }

    @Transactional
    public AdventureResponse moveRegion(String userId, String adventureId, String regionId) {
        Adventure adventure = findOwner(userId, adventureId);
        adventure.setRegion(resolveRegion(userId, regionId));
        log.info("Adventure {} moved to region {}", adventureId, regionId);
        return metricsAssembler.build(adventureRepository.save(adventure));
    }

    private Region resolveRegion(String userId, String regionId) {
        if (isNull(regionId) || regionId.isBlank()) {
            return null;
        }
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("regiao nao encontrada"));
        OwnershipValidator.requireOwner(userId, region.getUserId(), "Voce nao e o dono desta regiao");
        return region;
    }

    @Transactional(readOnly = true)
    public AdventureResponse getById(String observerId, String id) {
        Adventure adventure = findById(id);
        accessService.validateView(observerId, adventure);
        return metricsAssembler.build(adventure);
    }

    @Transactional(readOnly = true)
    public Page<AdventureResponse> getByUser(String observerId, String userId, Pageable pageable) {
        Page<Adventure> page = observerId.equals(userId)
                ? adventureRepository.findByUserId(userId, pageable)
                : adventureRepository.findVisibleByUser(userId, observerId, pageable);
        return metricsAssembler.buildPage(page);
    }

    /**
     * Feed: own adventures plus the visible ones from the users the observer
     * follows, newest first. The sentinel keeps the IN clause valid when the
     * observer follows nobody (same trick as RegionService.discover).
     */
    @Transactional(readOnly = true)
    public Page<AdventureResponse> getFeed(String observerId, Pageable pageable) {
        List<String> authors = followerRepository.findFollowedIds(observerId);
        List<String> filter = authors.isEmpty() ? List.of("__sem_seguidos__") : authors;
        return metricsAssembler.buildPage(adventureRepository.findFeed(observerId, filter, pageable));
    }

    public AdventureResponse updateStatus(String userId, String id, AdventureStatus status) {
        log.info("Updating status of adventure {} to {}", id, status);
        Adventure adventure = findOwner(userId, id);
        adventure.setStatus(status);
        return metricsAssembler.build(adventureRepository.save(adventure));
    }

    private Adventure findById(String id) {
        return adventureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aventura nao encontrada"));
    }

    private Adventure findOwner(String userId, String id) {
        Adventure adventure = findById(id);
        OwnershipValidator.requireOwner(userId, adventure.getUserId(), "Voce nao e o dono desta aventura");
        return adventure;
    }
}
