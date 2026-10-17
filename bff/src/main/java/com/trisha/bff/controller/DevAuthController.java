package com.trisha.bff.controller;

import com.trisha.bff.model.dto.request.DevLoginRequest;
import com.trisha.bff.model.dto.response.AuthenticationResponse;
import com.trisha.bff.service.UserBffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login de desenvolvimento ({@code @Profile("dev")}): em prod a rota nem existe,
 * espelhando o DevAuthController do Cadastro. Emite um token real sem credencial,
 * entao nunca pode ficar exposta em producao.
 */
@RestController
@RequestMapping("/bff/auth/dev-login")
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final UserBffService userService;

    @PostMapping
    public AuthenticationResponse devLogin(@RequestBody @Valid DevLoginRequest request) {
        return userService.devLogin(request);
    }
}
