package com.trail.Cadastro.service;

import com.trail.Cadastro.auth.IssuedToken;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.mapper.AuthenticationMapper;
import com.trail.Cadastro.mapper.UserMapper;
import com.trail.Cadastro.model.dto.response.AuthenticationResponse;
import com.trail.Cadastro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.isNull;

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

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Transactional
    public AuthenticationResponse login(String email, String name) {
        log.info("Dev login para {}", email);

        User user = userRepository.findByEmail(email);
        if (isNull(user)) {
            Long sequence = userRepository.nextSequence();
            user = userRepository.save(UserMapper.toEntityDev(email, name, sequence));
            log.info("Usuario dev criado: {} ({})", user.getUserCode(), user.getId());
        }

        IssuedToken token = tokenService.issue(user);
        return AuthenticationMapper.toResponse(user, token);
    }
}
