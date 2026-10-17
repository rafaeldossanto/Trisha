package com.app.APP.controller;

import com.app.APP.auth.AuthenticatedUser;
import com.app.APP.model.dto.request.RegionRequest;
import com.app.APP.model.dto.response.AdventureResponse;
import com.app.APP.model.dto.response.RegionResponse;
import com.app.APP.service.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/regiao")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @PostMapping
    public RegionResponse create(AuthenticatedUser user,
                                 @RequestBody @Valid RegionRequest request) {
        return regionService.create(user.id(), request);
    }

    @GetMapping
    public Page<RegionResponse> listMine(AuthenticatedUser user,
                                         Pageable pageable) {
        return regionService.listMine(user.id(), pageable);
    }

    @GetMapping("/descobrir")
    public Page<RegionResponse> discover(AuthenticatedUser user,
                                         Pageable pageable) {
        return regionService.discover(user.id(), pageable);
    }

    @GetMapping("/{id}")
    public RegionResponse getById(AuthenticatedUser user,
                                  @PathVariable String id) {
        return regionService.getById(user.id(), id);
    }

    @GetMapping("/{id}/aventuras")
    public Page<AdventureResponse> getAdventures(AuthenticatedUser user,
                                                 @PathVariable String id,
                                                 Pageable pageable) {
        return regionService.getAdventures(user.id(), id, pageable);
    }

    @PutMapping("/{id}")
    public RegionResponse update(AuthenticatedUser user,
                                 @PathVariable String id,
                                 @RequestBody @Valid RegionRequest request) {
        return regionService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(AuthenticatedUser user,
                       @PathVariable String id) {
        regionService.delete(user.id(), id);
    }
}
