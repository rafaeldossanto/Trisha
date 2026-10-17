package com.trail.Cadastro.listener;

import com.trail.Cadastro.model.event.UserRegisteredEvent;
import com.trail.Cadastro.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispara o email de confirmacao fora da requisicao de cadastro.
 *
 * AFTER_COMMIT de proposito: se a transacao do cadastro rolar para tras, nao
 * existe usuario e o email nao deve sair. Combinado com @Async, o POST /usuario
 * responde sem esperar o SMTP.
 *
 * Falha definitiva no envio (depois dos retries de EmailService) apenas registra
 * o erro: a conta fica PENDENTE e RegistrationCleanupJob a recolhe ao fim do
 * prazo, liberando o email para uma nova tentativa de cadastro. Lancar daqui nao
 * teria para onde propagar — a resposta HTTP ja foi enviada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            emailService.sendConfirmation(event.userId(), event.email());
        } catch (Exception e) {
            log.error("Falha definitiva ao enviar email de confirmacao para o usuario {}: {}",
                    event.userId(), e.getMessage());
        }
    }
}
