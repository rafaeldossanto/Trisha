package com.trail.Cadastro.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Entrega o email de confirmacao no SMTP, com retentativa.
 *
 * Fica separado de EmailService de proposito: o retry precisa cobrir SO o envio.
 * Retentar sendConfirmation inteiro gravaria um EmailConfirmation novo (e um
 * token novo) a cada tentativa. Precisa ser um bean a parte porque @Retryable
 * age por proxy e nao funciona em chamada interna da propria classe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmationMailer {

    private final JavaMailSender mailSender;

    @Value("${app.email.remetente}")
    private String sender;

    /**
     * Substitui a reentrega automatica de job que o orquestrador de processos
     * fazia: sem ela, uma instabilidade momentanea do SMTP deixaria a conta
     * presa em PENDENTE ate o prazo de confirmacao estourar.
     *
     * Usa o @Retryable nativo do Spring Framework 7 (nao o spring-retry, que e da
     * geracao anterior): 'maxRetries' conta as RETENTATIVAS, entao 2 aqui
     * significa 3 envios no total — 2s de espera, depois 4s.
     */
    @Retryable(
            includes = IllegalStateException.class,
            maxRetries = 2,
            delay = 2000,
            multiplier = 2,
            timeUnit = TimeUnit.MILLISECONDS)
    public void send(String recipient, String name, String link) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject("Confirme seu email - Trilha");
            helper.setText(buildBody(name, link), true);

            mailSender.send(message);
            log.info("Email de confirmacao enviado para: {}", recipient);
        } catch (MessagingException | MailException e) {
            log.warn("Falha ao enviar email de confirmacao para {}: {}", recipient, e.getMessage());
            throw new IllegalStateException("Nao foi possivel enviar o email de confirmacao", e);
        }
    }

    private String buildBody(String name, String link) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto; color: #1a1a1a;">
                  <h2>Bem-vindo ao Trilha, %s!</h2>
                  <p>Falta um passo para ativar sua conta. Clique no botao abaixo para confirmar seu email:</p>
                  <p style="text-align: center; margin: 32px 0;">
                    <a href="%s" style="background: #2e7d32; color: #ffffff; padding: 14px 28px;
                       text-decoration: none; border-radius: 8px; display: inline-block;">
                      Confirmar email
                    </a>
                  </p>
                  <p>Se o botao nao funcionar, copie e cole este link no navegador:</p>
                  <p><a href="%s">%s</a></p>
                  <p style="color: #888888; font-size: 13px;">O link expira em 10 minutos. Se voce nao criou uma conta no Trilha, ignore este email.</p>
                </div>
                """.formatted(name, link, link, link);
    }
}
