package com.trisha.bff.controller;

import com.trisha.bff.model.dto.request.RegionRequest;
import com.trisha.bff.model.dto.response.AdventureResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.RegionResponse;
import com.trisha.bff.service.RegionBffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/bff/regioes")
@RequiredArgsConstructor
public class RegionController {

    private final RegionBffService regionService;

    @PostMapping
    public RegionResponse create(@RequestBody @Valid RegionRequest request) {
        return regionService.create(request);
    }

    @GetMapping
    public PageResponse<RegionResponse> mine(Pageable pageable) {
        return regionService.mine(pageable);
    }

    @GetMapping("/descobrir")
    public PageResponse<RegionResponse> discover(Pageable pageable) {
        return regionService.discover(pageable);
    }

    @GetMapping("/{id}")
    public RegionResponse getById(@PathVariable String id) {
        return regionService.getById(id);
    }

    @GetMapping("/{id}/aventuras")
    public PageResponse<AdventureResponse> adventures(@PathVariable String id,
                                                      Pageable pageable) {
        return regionService.adventures(id, pageable);
    }

    @PutMapping("/{id}")
    public RegionResponse update(@PathVariable String id,
                                 @RequestBody @Valid RegionRequest request) {
        return regionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        regionService.delete(id);
    }
}
