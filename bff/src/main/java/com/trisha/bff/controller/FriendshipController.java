package com.trisha.bff.controller;

import com.trisha.bff.auth.AuthenticatedUser;
import com.trisha.bff.model.dto.request.FriendshipRequest;
import com.trisha.bff.model.dto.response.FriendshipResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.service.FriendshipBffService;
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

@RestController
@RequestMapping("/bff/amizades")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipBffService friendshipService;

    @PostMapping
    public FriendshipResponse request(@RequestBody @Valid FriendshipRequest request) {
        return friendshipService.request(request);
    }

    @PatchMapping("/{id}/responder")
    public FriendshipResponse respond(@PathVariable String id,
                                      @RequestParam String status) {
        return friendshipService.respond(id, status);
    }

    @GetMapping("/pendentes")
    public PageResponse<FriendshipResponse> getPending(AuthenticatedUser user,
                                                       Pageable pageable) {
        return friendshipService.getPending(user, pageable);
    }

    @GetMapping("/amigos")
    public PageResponse<FriendshipResponse> getFriends(AuthenticatedUser user,
                                                       Pageable pageable) {
        return friendshipService.getFriends(user, pageable);
    }
}
