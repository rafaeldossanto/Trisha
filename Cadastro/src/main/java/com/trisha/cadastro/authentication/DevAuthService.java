package com.trisha.cadastro.authentication;

import com.trisha.cadastro.account.User;
import com.trisha.cadastro.account.UserService;
import com.trisha.cadastro.authentication.dto.AuthenticationResponse;
import com.trisha.cadastro.token.IssuedToken;
import com.trisha.cadastro.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atalho de login para desenvolvimento: cria o usuario na primeira vez (ou o
 * recupera pelo email) e emite o token da app — sem provedor social nem senha.
 * Existe apenas no profile dev; em prod o bean nao e carregado.
 */
@Service
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAuthService {

    private final UserService userService;
    private final TokenService tokenService;

    @Transactional
    public AuthenticationResponse login(String email, String name) {
        log.info("Dev login para {}", email);

        User user = userService.findByEmail(email)
                .orElseGet(() -> createDevUser(email, name));

        IssuedToken token = tokenService.issue(user);
        return AuthenticationMapper.toResponse(user, token);
    }

    private User createDevUser(String email, String name) {
        User user = userService.createActive(name, email);
        log.info("Usuario dev criado: {} ({})", user.getUserCode(), user.getId());
        return user;
    }
}
