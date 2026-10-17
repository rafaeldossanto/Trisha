package com.app.APP.controller;

import com.app.APP.auth.AuthenticatedUser;
import com.app.APP.exception.ForbiddenException;
import com.app.APP.model.dto.request.FollowRequest;
import com.app.APP.model.dto.response.CountersResponse;
import com.app.APP.model.dto.response.FollowStatusResponse;
import com.app.APP.model.dto.response.PublicUserResponse;
import com.app.APP.service.FollowerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Target always identified by userCode (public handle): in the body for follow/unfollow,
 * in ?codigo= for GETs. Avoids '#' in the path. The service resolves the code to an id.
 */
@RestController
@RequestMapping("/seguidor")
@RequiredArgsConstructor
public class FollowerController {

    private final FollowerService followerService;

    @PostMapping
    public void follow(AuthenticatedUser user,
                       @RequestBody @Valid FollowRequest request) {
        followerService.follow(user.id(), request.followedCode());
    }

    @DeleteMapping
    public void unfollow(AuthenticatedUser user,
                         @RequestBody @Valid FollowRequest request) {
        followerService.unfollow(user.id(), request.followedCode());
    }

    @GetMapping("/seguidores")
    public Page<PublicUserResponse> getFollowers(@RequestParam String codigo,
                                                 Pageable pageable) {
        return followerService.getFollowers(codigo, pageable);
    }

    @GetMapping("/seguindo")
    public Page<PublicUserResponse> getFollowing(@RequestParam String codigo,
                                                 Pageable pageable) {
        return followerService.getFollowing(codigo, pageable);
    }

    @GetMapping("/contadores")
    public CountersResponse counters(@RequestParam String codigo) {
        return followerService.counters(codigo);
    }

    @GetMapping("/status")
    public FollowStatusResponse status(AuthenticatedUser user,
                                       @RequestParam String codigo) {
        return followerService.status(user.id(), codigo);
    }

    /** Follow check by ids — used by the Location service (SEGUIDORES visibility). */
    @GetMapping("/segue")
    public boolean isFollower(AuthenticatedUser user,
                              @RequestParam("seguidorId") String followerId,
                              @RequestParam("seguidoId") String followedId) {
        if (!user.id().equals(followerId) && !user.id().equals(followedId)) {
            throw new ForbiddenException("Consulta permitida apenas sobre a propria relacao");
        }
        return followerService.isFollower(followerId, followedId);
    }

    /** Ids de quem o autenticado segue — checagem em lote do loc (lista ao vivo). */
    @GetMapping("/seguindo-ids")
    public List<String> followingIds(AuthenticatedUser user) {
        return followerService.followingIds(user.id());
    }
}
