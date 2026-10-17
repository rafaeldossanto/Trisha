package com.trisha.cadastro.registration;

import com.trisha.cadastro.account.UserService;
import com.trisha.cadastro.account.dto.UserCreateRequest;
import com.trisha.cadastro.account.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste o usuario (UserService) e publica o evento que dispara o email de
 * confirmacao. O email sai fora da requisicao e so depois do commit, entao o
 * POST /usuario nao espera o SMTP.
 *
 * O restante do cadastro nao passa por aqui: confirmacao de email e aceite de
 * termos sao conduzidos por ConfirmationService, e o prazo de confirmacao por
 * RegistrationCleanupJob.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserDTO register(UserCreateRequest request) {
        UserDTO user = userService.create(request);

        eventPublisher.publishEvent(new UserRegisteredEvent(user.id(), user.email()));

        log.info("Cadastro iniciado para usuario: {}", user.id());
        return user;
    }
}
