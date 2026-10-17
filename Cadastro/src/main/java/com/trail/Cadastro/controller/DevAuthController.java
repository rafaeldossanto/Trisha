package com.trail.Cadastro.controller;

import com.trail.Cadastro.model.dto.request.DevLoginRequest;
import com.trail.Cadastro.model.dto.response.AuthenticationResponse;
import com.trail.Cadastro.service.DevAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login de desenvolvimento ({@code @Profile("dev")}): em prod a rota nem existe.
 */
@RestController
@RequestMapping("/auth/dev-login")
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevAuthService devAuthService;

    @PostMapping
    public AuthenticationResponse login(@RequestBody @Valid DevLoginRequest request) {
        return devAuthService.login(request.email(), request.name());
    }
}
