package com.trisha.app.point;

import com.trisha.app.adventure.AdventureAccessService;
import com.trisha.app.path.Path;
import com.trisha.app.path.PathRepository;
import com.trisha.app.path.PathService;
import com.trisha.app.point.dto.EvidenceRequest;
import com.trisha.app.point.dto.EvidenceResponse;
import com.trisha.app.point.dto.PointOfInterestRequest;
import com.trisha.app.point.dto.PointOfInterestResponse;
import com.trisha.app.shared.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointOfInterestService {

    private final PointOfInterestRepository pointRepository;
    private final EvidenceRepository evidenceRepository;
    private final PointOfInterestUserStatusRepository statusRepository;
    private final PathRepository pathRepository;
    private final PathService pathService;
    private final AdventureAccessService accessService;

    private static final double MAX_DISTANCE_METERS = 50.0;

    @Transactional
    public PointOfInterestResponse create(String userId, PointOfInterestRequest request) {
        log.info("Creating point of interest type: {}", request.type());

        Path path = pathRepository.findById(request.pathId())
                .orElseThrow(() -> new IllegalArgumentException("Caminho nao encontrado"));
        accessService.validateContribute(userId, path.getAdventure());

        PointOfInterest point = pointRepository.save(PointOfInterestMapper.toEntity(request, path, userId));

        return PointOfInterestMapper.toResponse(point, point.confidenceLevel(0));
    }

    @Transactional
    public EvidenceResponse addEvidence(String userId, EvidenceRequest request) {
        PointOfInterest point = pointRepository.findById(request.pointId())
                .orElseThrow(() -> new IllegalArgumentException("Ponto nao encontrado"));

        accessService.validateView(userId, point.getPath().getAdventure());

        double distance = GeoUtils.distanceMeters(
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
        return PointOfInterestMapper.toResponse(point,
                point.confidenceLevel(evidenceRepository.countValidatedUsersByPointId(point.getId())));
    }

    @Transactional(readOnly = true)
    public Page<PointOfInterestResponse> getByPath(String observerId, String pathId, Pageable pageable) {
        if (!pathService.canView(observerId, pathId)) {
            throw new IllegalArgumentException("Caminho nao encontrado ou sem acesso");
        }
        Page<PointOfInterest> page = pointRepository.findByPathId(pathId, pageable);

        Map<String, Long> validatedPerPoint = countValidatedInBatch(page.getContent());

        return page.map(p -> PointOfInterestMapper.toResponse(
                p, p.confidenceLevel(validatedPerPoint.getOrDefault(p.getId(), 0L))));
    }

    /**
     * Apaga os pontos de um conjunto de caminhos com o que pende deles, nesta
     * ordem: marcacoes pessoais, evidencias e os pontos.
     */
    @Transactional
    public void deleteByPathIds(List<String> pathIds) {
        if (pathIds.isEmpty()) {
            return;
        }
        List<PointOfInterest> points = pointRepository.findByPathIdIn(pathIds);
        List<String> pointIds = points.stream().map(PointOfInterest::getId).toList();
        if (!pointIds.isEmpty()) {
            statusRepository.deleteByPointIdIn(pointIds);
            evidenceRepository.deleteByPointIdIn(pointIds);
        }
        pointRepository.deleteAll(points);
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
}
