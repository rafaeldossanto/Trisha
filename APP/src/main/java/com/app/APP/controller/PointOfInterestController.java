package com.app.APP.controller;

import com.app.APP.auth.AuthenticatedUser;
import com.app.APP.model.dto.request.EvidenceRequest;
import com.app.APP.model.dto.request.PointOfInterestRequest;
import com.app.APP.model.dto.response.EvidenceResponse;
import com.app.APP.model.dto.response.PointOfInterestResponse;
import com.app.APP.model.dto.response.PointStatusResponse;
import com.app.APP.model.enums.PointStatus;
import com.app.APP.service.PointOfInterestService;
import com.app.APP.service.PointOfInterestStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ponto-interesse")
@RequiredArgsConstructor
public class PointOfInterestController {

    private final PointOfInterestService pointOfInterestService;
    private final PointOfInterestStatusService pointOfInterestStatusService;

    @PostMapping
    public PointOfInterestResponse create(AuthenticatedUser user,
                                          @RequestBody @Valid PointOfInterestRequest request) {
        return pointOfInterestService.create(user.id(), request);
    }

    @GetMapping("/{id}")
    public PointOfInterestResponse getById(AuthenticatedUser user,
                                           @PathVariable String id) {
        return pointOfInterestService.getById(user.id(), id);
    }

    @GetMapping("/caminho/{pathId}")
    public Page<PointOfInterestResponse> getByPath(AuthenticatedUser user,
                                                   @PathVariable String pathId,
                                                   Pageable pageable) {
        return pointOfInterestService.getByPath(user.id(), pathId, pageable);
    }

    @PostMapping("/evidencia")
    public EvidenceResponse addEvidence(AuthenticatedUser user,
                                        @RequestBody @Valid EvidenceRequest request) {
        return pointOfInterestService.addEvidence(user.id(), request);
    }

    @PatchMapping("/{id}/status")
    public PointStatusResponse setStatus(AuthenticatedUser user,
                                         @PathVariable String id,
                                         @RequestParam PointStatus status) {
        return pointOfInterestStatusService.setStatus(user.id(), id, status);
    }

    @DeleteMapping("/{id}/status")
    public void clearStatus(AuthenticatedUser user,
                            @PathVariable String id) {
        pointOfInterestStatusService.clearStatus(user.id(), id);
    }

    @PatchMapping("/{id}/objetivo")
    public PointStatusResponse setGoal(AuthenticatedUser user,
                                       @PathVariable String id,
                                       @RequestParam("objetivo") boolean goal) {
        return pointOfInterestStatusService.setGoal(user.id(), id, goal);
    }

    /** Status do usuario autenticado para os pontos visiveis no mapa, em lote. */
    @GetMapping("/status")
    public List<PointStatusResponse> getStatuses(AuthenticatedUser user,
                                                 @RequestParam("ids") List<String> ids) {
        return pointOfInterestStatusService.getStatuses(user.id(), ids);
    }
}
