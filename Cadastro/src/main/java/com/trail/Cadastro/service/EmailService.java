package com.trail.Cadastro.service;

import com.trail.Cadastro.entity.EmailConfirmation;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.mail.ConfirmationMailer;
import com.trail.Cadastro.model.enums.ConfirmationStatus;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.EmailConfirmationRepository;
import com.trail.Cadastro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final int RESEND_COOLDOWN_SECONDS = 30;

    private final UserRepository repository;
    private final EmailConfirmationRepository emailRepository;
    private final ConfirmationMailer mailer;

    @Value("${app.email.confirmacao-url}")
    private String confirmationUrl;

    public String sendConfirmation(String userId, String email) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado: " + userId));

        String token = UUID.randomUUID().toString();

        EmailConfirmation confirmation = EmailConfirmation.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .token(token)
                .status(ConfirmationStatus.PENDENTE)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .sentAt(LocalDateTime.now())
                .build();

        emailRepository.save(confirmation);

        mailer.send(email, user.getName(), confirmationUrl + "?token=" + token);

        return token;
    }

    /**
     * Reenvia o email de confirmacao para uma conta PENDENTE, respeitando o
     * intervalo minimo de 30s desde o ultimo envio. Gera um token novo; os
     * anteriores continuam validos ate expirarem. O reenvio NAO estende o prazo
     * do processo de cadastro — expirado o prazo, e preciso se cadastrar de novo.
     */
    public String resendConfirmation(String userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado: " + userId));

        if (RegistrationStatus.ATIVO.equals(user.getStatus())) {
            throw new IllegalArgumentException("Email ja confirmado");
        }
        if (RegistrationStatus.INATIVO.equals(user.getStatus())) {
            throw new IllegalArgumentException("Cadastro expirado, faca um novo cadastro");
        }

        emailRepository.findFirstByUserIdOrderBySentAtDesc(userId).ifPresent(this::validateCooldown);

        log.info("Reenviando email de confirmacao para usuario: {}", userId);
        return sendConfirmation(userId, user.getEmail());
    }

    private void validateCooldown(EmailConfirmation last) {
        if (isNull(last.getSentAt())) {
            return;
        }
        LocalDateTime allowedAt = last.getSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(allowedAt)) {
            long remaining = Math.max(1, Duration.between(now, allowedAt).getSeconds());
            throw new IllegalArgumentException(
                    "Aguarde " + remaining + " segundos para reenviar o email");
        }
    }

}
