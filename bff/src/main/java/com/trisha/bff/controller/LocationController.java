package com.trisha.bff.controller;

import com.trisha.bff.auth.AuthenticatedUser;
import com.trisha.bff.model.dto.request.GpsPointRequest;
import com.trisha.bff.model.dto.request.SessionRequest;
import com.trisha.bff.model.dto.response.GpsPointResponse;
import com.trisha.bff.model.dto.response.LiveSessionResponse;
import com.trisha.bff.model.dto.response.SessionResponse;
import com.trisha.bff.service.LocationBffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/bff/localizacao")
@RequiredArgsConstructor
public class LocationController {

    private final LocationBffService locationService;

    @PostMapping("/sessao")
    public SessionResponse startSession(@RequestBody @Valid SessionRequest request) {
        return locationService.startSession(request);
    }

    @PostMapping("/ponto")
    public GpsPointResponse registerPoint(@RequestBody @Valid GpsPointRequest request) {
        return locationService.registerPoint(request);
    }

    @PatchMapping("/sessao/{id}/finalizar")
    public SessionResponse finishSession(@PathVariable String id) {
        return locationService.finishSession(id);
    }

    @PatchMapping("/sessao/{id}/cancelar")
    public SessionResponse cancelSession(@PathVariable String id) {
        return locationService.cancelSession(id);
    }

    /** Troca quem acompanha a trilha ao vivo (PUBLICO/SEGUIDORES/AMIGOS/PRIVADO); so o dono. */
    @PatchMapping("/sessao/{id}/visibilidade")
    public SessionResponse updateVisibility(@PathVariable String id,
                                            @RequestParam("visibilidade") String visibility) {
        return locationService.updateVisibility(id, visibility);
    }

    @GetMapping("/sessao/caminho/{caminhoId}")
    public SessionResponse getSessionByPath(@PathVariable("caminhoId") String pathId) {
        return locationService.getSessionByPath(pathId);
    }

    /** Quem esta trilhando agora e o usuario pode acompanhar ao vivo. */
    @GetMapping("/sessoes-ao-vivo")
    public List<LiveSessionResponse> getLiveSessions() {
        return locationService.getLiveSessions();
    }

    @GetMapping("/pontos/sessao/{sessaoId}")
    public List<GpsPointResponse> getPointsBySession(AuthenticatedUser user,
                                                     @PathVariable("sessaoId") String sessionId) {
        return locationService.getPointsBySession(user.id(), sessionId);
    }

    @GetMapping("/pontos/caminho/{caminhoId}")
    public List<GpsPointResponse> getPointsByPath(AuthenticatedUser user,
                                                  @PathVariable("caminhoId") String pathId) {
        return locationService.getPointsByPath(user.id(), pathId);
    }
}
