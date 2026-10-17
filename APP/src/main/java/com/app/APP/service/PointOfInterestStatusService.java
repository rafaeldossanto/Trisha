package com.app.APP.service;

import com.app.APP.entity.PointOfInterest;
import com.app.APP.entity.PointOfInterestUserStatus;
import com.app.APP.mapper.PointOfInterestStatusMapper;
import com.app.APP.model.dto.response.PointStatusResponse;
import com.app.APP.model.enums.PointStatus;
import com.app.APP.repository.PointOfInterestRepository;
import com.app.APP.repository.PointOfInterestUserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointOfInterestStatusService {

    private final PointOfInterestUserStatusRepository statusRepository;
    private final PointOfInterestRepository pointRepository;
    private final AdventureAccessService accessService;

    @Transactional
    public PointStatusResponse setStatus(String userId, String pointId, PointStatus status) {
        log.info("Marking point {} with status {}", pointId, status);
        validateAccess(userId, pointId);

        PointOfInterestUserStatus mark = findOrNew(userId, pointId);
        mark.setStatus(status);

        return PointOfInterestStatusMapper.toResponse(statusRepository.save(mark));
    }

    @Transactional
    public void clearStatus(String userId, String pointId) {
        log.info("Clearing status of point {}", pointId);
        statusRepository.findByUserIdAndPointId(userId, pointId).ifPresent(mark -> {
            mark.setStatus(null);
            saveOrRemoveIfEmpty(mark);
        });
    }

    @Transactional
    public PointStatusResponse setGoal(String userId, String pointId, boolean goal) {
        log.info("Setting goal={} on point {}", goal, pointId);
        if (!goal) {
            return unsetGoal(userId, pointId);
        }
        validateAccess(userId, pointId);

        PointOfInterestUserStatus mark = findOrNew(userId, pointId);
        mark.setGoal(true);

        return PointOfInterestStatusMapper.toResponse(statusRepository.save(mark));
    }

    @Transactional(readOnly = true)
    public List<PointStatusResponse> getStatuses(String userId, List<String> pointIds) {
        return statusRepository.findByUserIdAndPointIdIn(userId, pointIds).stream()
                .map(PointOfInterestStatusMapper::toResponse)
                .toList();
    }

    /**
     * Mesmo gate da evidencia: quem nao pode ver o ponto tambem nao pode
     * marca-lo. Remocoes nao passam por aqui — apagar a propria marcacao
     * continua possivel mesmo se o acesso a aventura mudou depois.
     */
    private void validateAccess(String userId, String pointId) {
        PointOfInterest point = pointRepository.findById(pointId)
                .orElseThrow(() -> new IllegalArgumentException("Ponto nao encontrado"));
        accessService.validateView(userId, point.getPath().getAdventure());
    }

    private PointOfInterestUserStatus findOrNew(String userId, String pointId) {
        return statusRepository.findByUserIdAndPointId(userId, pointId)
                .orElseGet(() -> PointOfInterestStatusMapper.toEntity(userId, pointId));
    }

    private PointStatusResponse unsetGoal(String userId, String pointId) {
        return statusRepository.findByUserIdAndPointId(userId, pointId)
                .map(mark -> {
                    mark.setGoal(false);
                    return PointOfInterestStatusMapper.toResponse(saveOrRemoveIfEmpty(mark));
                })
                .orElseGet(() -> emptyResponse(pointId));
    }

    /** Linha zerada (sem status e sem objetivo) nao fica no banco. */
    private PointOfInterestUserStatus saveOrRemoveIfEmpty(PointOfInterestUserStatus mark) {
        if (isNull(mark.getStatus()) && !mark.isGoal()) {
            statusRepository.delete(mark);
            return mark;
        }
        return statusRepository.save(mark);
    }

    private PointStatusResponse emptyResponse(String pointId) {
        return PointStatusResponse.builder()
                .pointId(pointId)
                .build();
    }
}
