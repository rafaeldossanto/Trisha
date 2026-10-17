package com.trisha.cadastro.authentication;

import com.trisha.cadastro.authentication.dto.AuthenticationResponse;
import com.trisha.cadastro.authentication.dto.LoginRequest;
import com.trisha.cadastro.authentication.dto.SocialLoginRequest;
import com.trisha.cadastro.authentication.social.SocialAuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticacao de usuarios. No login social o app envia o ID token obtido do
 * provedor; o backend valida, resolve o usuario (criado ou vinculado) e devolve
 * o usuario junto do access token da aplicacao. No login por senha, as
 * credenciais sao validadas contra o cadastro local (conta precisa estar ATIVA,
 * ou seja, com o email confirmado). Em ambos, o app envia o access token no
 * header Authorization a seguir.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final SocialAuthenticationService socialAuthenticationService;
    private final LoginService loginService;

    @PostMapping("/social")
    public AuthenticationResponse socialLogin(@RequestBody @Valid SocialLoginRequest request) {
        return socialAuthenticationService.authenticate(request.provider(), request.idToken());
    }

    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody @Valid LoginRequest request) {
        return loginService.login(request.email(), request.password());
    }
}
