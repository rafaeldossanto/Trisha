package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Region;
import com.app.APP.mapper.AdventureMapper;
import com.app.APP.mapper.ParticipantMapper;
import com.app.APP.model.dto.request.AdventureRequest;
import com.app.APP.model.dto.response.AdventureResponse;
import com.app.APP.model.enums.AdventureStatus;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.AdventureParticipantRepository;
import com.app.APP.repository.FollowerRepository;
import com.app.APP.repository.RegionRepository;
import com.app.APP.util.OwnershipValidator;
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
    private final AdventureExitService adventureExitService;
    private final AdventureMetricsAssembler metricsAssembler;

    @Transactional
    public AdventureResponse create(String userId, AdventureRequest request) {
        log.info("Creating adventure for user: {}", userId);

        Region region = resolveRegion(userId, request.regionId());

        var adventure = adventureRepository.save(AdventureMapper.toEntity(request, region, userId));

        participantRepository.save(ParticipantMapper.toEntity(adventure, userId));

        log.info("Adventure created with id: {}", adventure.getId());
        return metricsAssembler.build(adventure);
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

    public void addParticipant(String ownerId, String adventureId, String userId) {
        Adventure adventure = findOwner(ownerId, adventureId);

        if (participantRepository.existsByAdventureIdAndUserId(adventureId, userId)) {
            throw new IllegalArgumentException("Usuario ja participa dessa aventura");
        }

        participantRepository.save(ParticipantMapper.toEntity(adventure, userId));
        log.info("User {} added to adventure {}", userId, adventureId);
    }

    @Transactional
    public void delete(String userId, String id) {
        Adventure adventure = findOwner(userId, id);
        adventureExitService.deleteWithContent(adventure);
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
