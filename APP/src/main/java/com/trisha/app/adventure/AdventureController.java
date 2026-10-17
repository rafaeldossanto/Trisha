package com.trisha.app.adventure;

import com.trisha.app.adventure.dto.AdventureRequest;
import com.trisha.app.adventure.dto.AdventureResponse;
import com.trisha.app.adventure.dto.LeaveAdventureResponse;
import com.trisha.app.adventure.dto.MoveRegionRequest;
import com.trisha.app.shared.security.AuthenticatedUser;
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
    private final AdventureMembershipService membershipService;

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
        membershipService.addParticipant(user.id(), id, userId);
    }

    /** O proprio usuario sai da aventura em grupo; manterDados preserva os registros. */
    @DeleteMapping("/{id}/participante")
    public LeaveAdventureResponse leave(AuthenticatedUser user,
                                        @PathVariable String id,
                                        @RequestParam(defaultValue = "true") boolean manterDados) {
        return membershipService.leave(user.id(), id, manterDados);
    }

    /** O dono remove um participante; os dados do removido sao sempre preservados. */
    @DeleteMapping("/{id}/participante/{userId}")
    public LeaveAdventureResponse kick(AuthenticatedUser user,
                                       @PathVariable String id,
                                       @PathVariable String userId) {
        return membershipService.kick(user.id(), id, userId);
    }

    @DeleteMapping("/{id}")
    public void delete(AuthenticatedUser user,
                       @PathVariable String id) {
        membershipService.delete(user.id(), id);
    }
}
