package com.app.APP.service;

import com.app.APP.entity.Evidence;
import com.app.APP.entity.Path;
import com.app.APP.entity.PointOfInterest;
import com.app.APP.mapper.PointOfInterestMapper;
import com.app.APP.model.dto.request.EvidenceRequest;
import com.app.APP.model.dto.request.PointOfInterestRequest;
import com.app.APP.model.dto.response.EvidenceResponse;
import com.app.APP.model.dto.response.PointOfInterestResponse;
import com.app.APP.repository.PathRepository;
import com.app.APP.repository.EvidenceRepository;
import com.app.APP.repository.PointOfInterestRepository;
import com.app.APP.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointOfInterestService {

    private final PointOfInterestRepository pointRepository;
    private final EvidenceRepository evidenceRepository;
    private final PathRepository pathRepository;
    private final AdventureAccessService accessService;

    private static final double MAX_DISTANCE_METERS = 50.0;

    @Transactional
    public PointOfInterestResponse create(String userId, PointOfInterestRequest request) {
        log.info("Creating point of interest type: {}", request.type());

        Path path = pathRepository.findById(request.pathId())
                .orElseThrow(() -> new IllegalArgumentException("Caminho nao encontrado"));
        accessService.validateContribute(userId, path.getAdventure());

        PointOfInterest point = pointRepository.save(PointOfInterestMapper.toEntity(request, path, userId));

        return PointOfInterestMapper.toResponse(point, calculateLevel(point, 0));
    }

    @Transactional
    public EvidenceResponse addEvidence(String userId, EvidenceRequest request) {
        PointOfInterest point = pointRepository.findById(request.pointId())
                .orElseThrow(() -> new IllegalArgumentException("Ponto nao encontrado"));

        accessService.validateView(userId, point.getPath().getAdventure());

        double distance = GeoUtils.distanciaMetros(
                point.getLatitude(), point.getLongitude(),
                request.captureLat(), request.captureLng()
        );

        if (distance > MAX_DISTANCE_METERS) {
            log.warn("Evidence rejected — distance from point: {}m (limit: {}m)", distance, MAX_DISTANCE_METERS);
            throw new IllegalArgumentException("Voce esta muito longe do ponto para adicionar evidencia. Distancia: " + (int) distance + "m");
        }

        Evidence evidence = evidenceRepository.save(
                PointOfInterestMapper.toEvidenceEntity(request, point, distance, userId)
        );
        log.info("Evidence validated and saved for point: {}", point.getId());

        return PointOfInterestMapper.toEvidenceResponse(evidence);
    }

    @Transactional(readOnly = true)
    public PointOfInterestResponse getById(String observerId, String id) {
        PointOfInterest point = pointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ponto nao encontrado"));
        accessService.validateView(observerId, point.getPath().getAdventure());
        return PointOfInterestMapper.toResponse(point, calculateLevel(point));
    }

    @Transactional(readOnly = true)
    public Page<PointOfInterestResponse> getByPath(String observerId, String pathId, Pageable pageable) {
        if (!accessService.canViewPath(observerId, pathId)) {
            throw new IllegalArgumentException("Caminho nao encontrado ou sem acesso");
        }
        Page<PointOfInterest> page = pointRepository.findByPathId(pathId, pageable);

        Map<String, Long> validatedPerPoint = countValidatedInBatch(page.getContent());

        return page.map(p -> PointOfInterestMapper.toResponse(
                p, calculateLevel(p, validatedPerPoint.getOrDefault(p.getId(), 0L))));
    }

    private Map<String, Long> countValidatedInBatch(List<PointOfInterest> points) {
        if (points.isEmpty()) {
            return Map.of();
        }
        List<String> ids = points.stream().map(PointOfInterest::getId).toList();
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : evidenceRepository.countValidatedUsersPerPoint(ids)) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    private int calculateLevel(PointOfInterest point) {
        return calculateLevel(point, evidenceRepository.countValidatedUsersByPointId(point.getId()));
    }

    private int calculateLevel(PointOfInterest point, long distinctUsers) {
        boolean hasDescription = nonNull(point.getDescription()) && !point.getDescription().isBlank();

        if (distinctUsers >= 3 && hasDescription) return 5;
        if (distinctUsers >= 2 && hasDescription) return 4;
        if (distinctUsers >= 1 && hasDescription) return 3;
        if (hasDescription) return 2;
        return 1;
    }
}
