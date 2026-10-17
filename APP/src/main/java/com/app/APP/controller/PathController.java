package com.app.APP.controller;

import com.app.APP.auth.AuthenticatedUser;
import com.app.APP.model.dto.request.PathRequest;
import com.app.APP.model.dto.response.PathDiscoveryResponse;
import com.app.APP.model.dto.response.PathResponse;
import com.app.APP.service.PathService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/caminho")
@RequiredArgsConstructor
public class PathController {

    private final PathService pathService;

    @PostMapping
    public PathResponse start(AuthenticatedUser user,
                              @RequestBody @Valid PathRequest request) {
        return pathService.start(user.id(), request);
    }

    @PatchMapping("/{id}/finalizar")
    public PathResponse finish(AuthenticatedUser user,
                               @PathVariable String id,
                               @RequestParam Double totalDistanceKm) {
        return pathService.finish(user.id(), id, totalDistanceKm);
    }

    @GetMapping("/aventura/{adventureId}")
    public Page<PathResponse> getByAdventure(AuthenticatedUser user,
                                             @PathVariable String adventureId,
                                             Pageable pageable) {
        return pathService.getByAdventure(user.id(), adventureId, pageable);
    }

    @GetMapping("/usuario/{userId}")
    public Page<PathResponse> getByUser(AuthenticatedUser user,
                                        @PathVariable String userId,
                                        Pageable pageable) {
        return pathService.getByUser(user.id(), userId, pageable);
    }

    @GetMapping("/{id}/acesso")
    public boolean canView(AuthenticatedUser user,
                           @PathVariable String id) {
        return pathService.canView(user.id(), id);
    }

    /** Quais destes caminhos o usuario autenticado pode ver no mapa colaborativo. */
    @GetMapping("/descobrir")
    public List<PathDiscoveryResponse> discover(AuthenticatedUser user,
                                                @RequestParam("ids") List<String> ids) {
        return pathService.discover(user.id(), ids);
    }
}
