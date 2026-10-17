package com.trisha.bff.controller;

import com.trisha.bff.auth.AuthenticatedUser;
import com.trisha.bff.model.dto.request.MediaRequest;
import com.trisha.bff.model.dto.response.MediaResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.service.MediaBffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/midias")
@RequiredArgsConstructor
public class MediaController {

    private final MediaBffService mediaService;

    @PostMapping
    public MediaResponse save(@RequestBody @Valid MediaRequest request) {
        return mediaService.save(request);
    }

    @GetMapping("/aventura/{aventuraId}")
    public PageResponse<MediaResponse> getByAdventure(AuthenticatedUser user,
                                                      @PathVariable("aventuraId") String adventureId,
                                                      Pageable pageable) {
        return mediaService.getByAdventure(user.id(), adventureId, pageable);
    }

    @GetMapping("/caminho/{caminhoId}")
    public PageResponse<MediaResponse> getByPath(AuthenticatedUser user,
                                                 @PathVariable("caminhoId") String pathId,
                                                 Pageable pageable) {
        return mediaService.getByPath(user.id(), pathId, pageable);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        mediaService.delete(id);
    }
}
