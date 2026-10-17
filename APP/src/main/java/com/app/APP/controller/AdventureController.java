package com.app.APP.controller;

import com.app.APP.auth.AuthenticatedUser;
import com.app.APP.model.dto.request.AdventureRequest;
import com.app.APP.model.dto.request.MoveRegionRequest;
import com.app.APP.model.dto.response.AdventureResponse;
import com.app.APP.model.dto.response.LeaveAdventureResponse;
import com.app.APP.model.enums.AdventureStatus;
import com.app.APP.service.AdventureExitService;
import com.app.APP.service.AdventureService;
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

@RestController
@RequestMapping("/aventura")
@RequiredArgsConstructor
public class AdventureController {

    private final AdventureService adventureService;
    private final AdventureExitService adventureExitService;

    @PostMapping
    public AdventureResponse create(AuthenticatedUser user,
                                    @RequestBody @Valid AdventureRequest request) {
        return adventureService.create(user.id(), request);
    }

    @GetMapping("/{id}")
    public AdventureResponse getById(AuthenticatedUser user,
                                     @PathVariable String id) {
        return adventureService.getById(user.id(), id);
    }

    @GetMapping("/usuario/{userId}")
    public Page<AdventureResponse> getByUser(AuthenticatedUser user,
                                             @PathVariable String userId,
                                             Pageable pageable) {
        return adventureService.getByUser(user.id(), userId, pageable);
    }

    /** Feed: minhas aventuras + as visiveis de quem eu sigo, mais recentes primeiro. */
    @GetMapping("/feed")
    public Page<AdventureResponse> getFeed(AuthenticatedUser user,
                                           Pageable pageable) {
        return adventureService.getFeed(user.id(), pageable);
    }

    @PatchMapping("/{id}/status")
    public AdventureResponse updateStatus(AuthenticatedUser user,
                                          @PathVariable String id,
                                          @RequestParam AdventureStatus status) {
        return adventureService.updateStatus(user.id(), id, status);
    }

    @PatchMapping("/{id}/regiao")
    public AdventureResponse moveRegion(AuthenticatedUser user,
                                        @PathVariable String id,
                                        @RequestBody MoveRegionRequest request) {
        return adventureService.moveRegion(user.id(), id, request.regionId());
    }

    @PostMapping("/{id}/participante")
    public void addParticipant(AuthenticatedUser user,
                               @PathVariable String id,
                               @RequestParam String userId) {
        adventureService.addParticipant(user.id(), id, userId);
    }

    /** O proprio usuario sai da aventura em grupo; manterDados preserva os registros. */
    @DeleteMapping("/{id}/participante")
    public LeaveAdventureResponse leave(AuthenticatedUser user,
                                        @PathVariable String id,
                                        @RequestParam(defaultValue = "true") boolean manterDados) {
        return adventureExitService.leave(user.id(), id, manterDados);
    }

    /** O dono remove um participante; os dados do removido sao sempre preservados. */
    @DeleteMapping("/{id}/participante/{userId}")
    public LeaveAdventureResponse kick(AuthenticatedUser user,
                                       @PathVariable String id,
                                       @PathVariable String userId) {
        return adventureExitService.kick(user.id(), id, userId);
    }

    @DeleteMapping("/{id}")
    public void delete(AuthenticatedUser user,
                       @PathVariable String id) {
        adventureService.delete(user.id(), id);
    }
}
