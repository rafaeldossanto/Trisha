package com.trail.Cadastro.service;

import com.trail.Cadastro.entity.EmailConfirmation;
import com.trail.Cadastro.entity.TermsAcceptance;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.model.enums.ConfirmationStatus;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.EmailConfirmationRepository;
import com.trail.Cadastro.repository.TermsAcceptanceRepository;
import com.trail.Cadastro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Conduz o cadastro ate a ativacao: a conta so vira ATIVA quando o email foi
 * confirmado E os termos foram aceitos. As duas etapas correm em paralelo e em
 * qualquer ordem — cada uma registra o proprio passo e tenta fechar o cadastro;
 * a ultima a chegar e quem ativa.
 *
 * O prazo de confirmacao e cobrado por RegistrationCleanupJob.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmationService {

    private final EmailConfirmationRepository confirmationRepository;
    private final TermsAcceptanceRepository termsRepository;
    private final UserRepository userRepository;

    @Transactional
    public void confirmEmail(String token) {
        // O token NAO vai para o log: ele e a credencial que ativa a conta. Quem
        // lesse o log (ou um agregador de logs) poderia confirmar conta alheia.
        log.info("Confirmando email");

        EmailConfirmation confirmation = confirmationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalido"));

        if (ConfirmationStatus.CONFIRMADO.equals(confirmation.getStatus())) {
            throw new IllegalArgumentException("Email ja confirmado");
        }

        if (confirmation.getExpiresAt().isBefore(LocalDateTime.now())) {
            confirmation.setStatus(ConfirmationStatus.EXPIRADO);
            confirmationRepository.save(confirmation);
            throw new IllegalArgumentException("Token expirado");
        }

        // Um token reenviado pode sobreviver a conta (a limpeza desativa o
        // pendente ao fim do prazo) ou chegar depois da ativacao por outro
        // token/login social: valida o status do dono antes de avancar.
        RegistrationStatus userStatus = confirmation.getUser().getStatus();
        if (RegistrationStatus.ATIVO.equals(userStatus)) {
            throw new IllegalArgumentException("Email ja confirmado");
        }
        if (RegistrationStatus.INATIVO.equals(userStatus)) {
            throw new IllegalArgumentException("Cadastro expirado, faca um novo cadastro");
        }

        confirmation.setStatus(ConfirmationStatus.CONFIRMADO);
        confirmation.setConfirmedAt(LocalDateTime.now());
        confirmationRepository.save(confirmation);

        String userId = confirmation.getUser().getId();
        tryActivate(userId);

        log.info("Email confirmado para usuario: {}", userId);
    }

    @Transactional
    public void acceptTerms(String userId) {
        log.info("Aceitando termos para usuario: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        TermsAcceptance terms = TermsAcceptance.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .accepted(true)
                .acceptedAt(LocalDateTime.now())
                .build();

        termsRepository.save(terms);

        tryActivate(userId);

        log.info("Termos aceitos para usuario: {}", userId);
    }

    /**
     * Fecha o cadastro se as duas etapas ja estiverem cumpridas; caso contrario
     * nao faz nada e quem completar a etapa que falta ativa depois.
     *
     * A ativacao em si e um UPDATE condicional (activateIfPending), entao duas
     * chamadas concorrentes — email e termos chegando ao mesmo tempo — nunca
     * ativam em duplicidade: o banco arbitra e a segunda afeta zero linhas.
     * Roda na mesma transacao da etapa que a disparou: ou a etapa e a ativacao
     * sao gravadas juntas, ou nenhuma das duas e.
     */
    private void tryActivate(String userId) {
        boolean emailConfirmado =
                confirmationRepository.existsByUserIdAndStatus(userId, ConfirmationStatus.CONFIRMADO);
        if (!emailConfirmado) {
            log.info("Cadastro {} aguardando confirmacao de email", userId);
            return;
        }

        boolean termosAceitos = termsRepository.existsByUserIdAndAcceptedTrue(userId);
        if (!termosAceitos) {
            log.info("Cadastro {} aguardando aceite dos termos", userId);
            return;
        }

        int ativados = userRepository.activateIfPending(userId, LocalDateTime.now());
        if (ativados > 0) {
            log.info("Cadastro concluido, conta ativada: {}", userId);
        } else {
            log.info("Conta {} ja estava ativa ou nao esta mais pendente", userId);
        }
    }
}
