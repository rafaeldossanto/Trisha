package com.app.APP.controller;

import com.app.APP.auth.AuthenticatedUser;
import com.app.APP.exception.ForbiddenException;
import com.app.APP.model.dto.request.FriendshipRequest;
import com.app.APP.model.dto.response.FriendshipResponse;
import com.app.APP.model.enums.FriendshipStatus;
import com.app.APP.service.FriendshipService;
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
@RequestMapping("/amizade")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping
    public FriendshipResponse request(AuthenticatedUser user,
                                      @RequestBody @Valid FriendshipRequest request) {
        return friendshipService.request(user.id(), request);
    }

    @PatchMapping("/{id}/responder")
    public FriendshipResponse respond(AuthenticatedUser user,
                                      @PathVariable String id,
                                      @RequestParam FriendshipStatus status) {
        return friendshipService.respond(user.id(), id, status);
    }

    @DeleteMapping("/{id}/solicitacao")
    public void cancelRequest(AuthenticatedUser user,
                              @PathVariable String id) {
        friendshipService.cancelRequest(user.id(), id);
    }

    @DeleteMapping("/{id}")
    public void unfriend(AuthenticatedUser user,
                         @PathVariable String id) {
        friendshipService.unfriend(user.id(), id);
    }

    @PostMapping("/bloquear")
    public FriendshipResponse block(AuthenticatedUser user,
                                    @RequestBody @Valid FriendshipRequest request) {
        return friendshipService.block(user.id(), request);
    }

    @DeleteMapping("/{id}/bloqueio")
    public void unblock(AuthenticatedUser user,
                        @PathVariable String id) {
        friendshipService.unblock(user.id(), id);
    }

    @GetMapping("/pendentes")
    public Page<FriendshipResponse> getPending(AuthenticatedUser user,
                                               Pageable pageable) {
        return friendshipService.getPending(user.id(), pageable);
    }

    @GetMapping("/enviadas")
    public Page<FriendshipResponse> getSent(AuthenticatedUser user,
                                            Pageable pageable) {
        return friendshipService.getSent(user.id(), pageable);
    }

    @GetMapping("/amigos")
    public Page<FriendshipResponse> getFriends(AuthenticatedUser user,
                                               Pageable pageable) {
        return friendshipService.getFriends(user.id(), pageable);
    }

    /** Friendship check between two users — used by the Location service (AMIGOS visibility). */
    @GetMapping("/sao-amigos")
    public boolean areFriends(AuthenticatedUser user,
                              @RequestParam String a,
                              @RequestParam String b) {
        if (!user.id().equals(a) && !user.id().equals(b)) {
            throw new ForbiddenException("Consulta permitida apenas sobre a propria relacao");
        }
        return friendshipService.areFriends(a, b);
    }

    /** Ids dos amigos do autenticado — checagem em lote do loc (lista ao vivo). */
    @GetMapping("/amigos-ids")
    public List<String> friendIds(AuthenticatedUser user) {
        return friendshipService.friendIds(user.id());
    }
}
