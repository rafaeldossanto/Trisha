package com.trisha.bff.controller;

import com.trisha.bff.auth.AuthenticatedUser;
import com.trisha.bff.model.dto.request.PathRequest;
import com.trisha.bff.model.dto.response.PathResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.TrailDiscoveryResponse;
import com.trisha.bff.service.PathBffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/bff/caminhos")
@RequiredArgsConstructor
public class PathController {

    private final PathBffService pathService;

    @PostMapping
    public PathResponse start(@RequestBody @Valid PathRequest request) {
        return pathService.start(request);
    }

    // A distancia nao vem mais do cliente: o BFF a obtem do servico de Localizacao.
    @PatchMapping("/{id}/finalizar")
    public PathResponse finish(@PathVariable String id) {
        return pathService.finish(id);
    }

    @GetMapping("/aventura/{aventuraId}")
    public PageResponse<PathResponse> getByAdventure(AuthenticatedUser user,
                                                     @PathVariable("aventuraId") String adventureId,
                                                     Pageable pageable) {
        return pathService.getByAdventure(user.id(), adventureId, pageable);
    }

    @GetMapping("/usuario/{usuarioId}")
    public PageResponse<PathResponse> getByUser(AuthenticatedUser user,
                                                @PathVariable("usuarioId") String userId,
                                                Pageable pageable) {
        return pathService.getByUser(user.id(), userId, pageable);
    }

    /**
     * Trilhas de outros usuarios visiveis na area do mapa (bbox do viewport).
     * O app chama a cada pan/zoom — por isso a geometria volta decimada
     * (limitePorCaminho) e restrita a area, nunca o mapa inteiro.
     */
    @GetMapping("/descobrir")
    public List<TrailDiscoveryResponse> discover(@RequestParam Double minLat,
                                                 @RequestParam Double minLng,
                                                 @RequestParam Double maxLat,
                                                 @RequestParam Double maxLng,
                                                 @RequestParam(name = "limitePorCaminho", defaultValue = "200")
                                                 Integer maxPointsPerPath) {
        return pathService.discover(minLat, minLng, maxLat, maxLng, maxPointsPerPath);
    }
}
