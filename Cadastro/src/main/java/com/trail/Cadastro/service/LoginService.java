package com.trail.Cadastro.service;

import com.trail.Cadastro.auth.IssuedToken;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.mapper.AuthenticationMapper;
import com.trail.Cadastro.model.dto.response.AuthenticationResponse;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;

/**
 * Login por email e senha. A senha informada e comparada com o hash BCrypt
 * persistido no cadastro. Usuarios sociais tem senha nula e nao autenticam aqui.
 *
 * <p><b>Resposta unica para toda falha.</b> Email inexistente, senha errada,
 * conta INATIVA e conta PENDENTE devolvem a MESMA mensagem. Distinguir os casos
 * permitiria enumerar quais emails estao cadastrados na base — basta tentar
 * login com uma lista e separar as respostas.
 *
 * <p>O custo e de UX: quem se cadastrou e nao confirmou o email nao descobre o
 * motivo por aqui. Por isso a tela de login precisa manter visivel a opcao de
 * reenviar a confirmacao ({@code POST /auth/reenviar-email}), que continua
 * informando o estado real para quem prova ser o dono do endereco.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    /** Mensagem unica de falha — nao revela se o email existe nem o estado da conta. */
    private static final String CREDENCIAL_INVALIDA = "Email ou senha invalidos";

    public AuthenticationResponse login(String email, String password) {
        log.info("Login por senha para {}", email);

        loginAttemptService.assertNotBlocked(email);

        User user = userRepository.findByEmail(email);
        if (isNull(user) || isNull(user.getPassword()) || !passwordEncoder.matches(password, user.getPassword())
                || RegistrationStatus.INATIVO.equals(user.getStatus())
                || RegistrationStatus.PENDENTE.equals(user.getStatus())) {
            // A conta PENDENTE entra aqui de proposito: informar "confirme seu
            // email" confirmaria ao atacante que o endereco esta cadastrado.
            loginAttemptService.recordFailure(email);
            throw new IllegalArgumentException(CREDENCIAL_INVALIDA);
        }

        loginAttemptService.reset(email);

        IssuedToken token = tokenService.issue(user);
        return AuthenticationMapper.toResponse(user, token);
    }
}
