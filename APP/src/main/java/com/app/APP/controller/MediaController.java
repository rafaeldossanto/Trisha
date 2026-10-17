package com.app.APP.controller;

import com.app.APP.auth.AuthenticatedUser;
import com.app.APP.model.dto.request.MediaRequest;
import com.app.APP.model.dto.response.MediaResponse;
import com.app.APP.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/midia")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping
    public MediaResponse save(AuthenticatedUser user,
                              @RequestBody @Valid MediaRequest request) {
        return mediaService.save(user.id(), request);
    }

    @GetMapping("/aventura/{adventureId}")
    public Page<MediaResponse> getByAdventure(AuthenticatedUser user,
                                              @PathVariable String adventureId,
                                              Pageable pageable) {
        return mediaService.getByAdventure(user.id(), adventureId, pageable);
    }

    @GetMapping("/caminho/{pathId}")
    public Page<MediaResponse> getByPath(AuthenticatedUser user,
                                         @PathVariable String pathId,
                                         Pageable pageable) {
        return mediaService.getByPath(user.id(), pathId, pageable);
    }

    @DeleteMapping("/{id}")
    public void delete(AuthenticatedUser user, @PathVariable String id) {
        mediaService.delete(user.id(), id);
    }
}
