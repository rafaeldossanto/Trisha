package com.trisha.bff.controller;

import com.trisha.bff.model.dto.request.FollowRequest;
import com.trisha.bff.model.dto.response.CountersResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.FollowStatusResponse;
import com.trisha.bff.model.dto.response.PublicUserResponse;
import com.trisha.bff.service.FollowerBffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/seguidores")
@RequiredArgsConstructor
public class FollowerController {

    private final FollowerBffService followerService;

    @PostMapping
    public void follow(@RequestBody @Valid FollowRequest request) {
        followerService.follow(request.followedCode());
    }

    @DeleteMapping
    public void unfollow(@RequestBody @Valid FollowRequest request) {
        followerService.unfollow(request.followedCode());
    }

    @GetMapping("/seguidores")
    public PageResponse<PublicUserResponse> followers(@RequestParam("codigo") String code,
                                                      Pageable pageable) {
        return followerService.followers(code, pageable);
    }

    @GetMapping("/seguindo")
    public PageResponse<PublicUserResponse> following(@RequestParam("codigo") String code,
                                                      Pageable pageable) {
        return followerService.following(code, pageable);
    }

    @GetMapping("/contadores")
    public CountersResponse counters(@RequestParam("codigo") String code) {
        return followerService.counters(code);
    }

    @GetMapping("/status")
    public FollowStatusResponse status(@RequestParam("codigo") String code) {
        return followerService.status(code);
    }
}
