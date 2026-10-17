package com.trail.Cadastro.service;

import com.trail.Cadastro.entity.EmailConfirmation;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.mail.ConfirmationMailer;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.model.enums.ConfirmationStatus;
import com.trail.Cadastro.repository.EmailConfirmationRepository;
import com.trail.Cadastro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    private static final String CONFIRMATION_URL = "http://localhost:8080/auth/confirmar-email";

    @Mock
    private UserRepository repository;
    @Mock
    private EmailConfirmationRepository emailRepository;
    @Mock
    private ConfirmationMailer mailer;

    @InjectMocks
    private EmailService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "confirmationUrl", CONFIRMATION_URL);
    }

    private User userStub() {
        return User.builder()
                .id("id-123")
                .name("Rafael")
                .email("rafael@email.com")
                .status(RegistrationStatus.PENDENTE)
                .build();
    }

    private void verificaQueNaoEnviou() {
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendConfirmation deve persistir token PENDENTE, enviar o email e retornar o token")
    void deveEnviarConfirmacao() {
        when(repository.findById("id-123")).thenReturn(Optional.of(userStub()));

        String token = service.sendConfirmation("id-123", "rafael@email.com");

        assertThat(token).isNotBlank();

        ArgumentCaptor<EmailConfirmation> captor = ArgumentCaptor.forClass(EmailConfirmation.class);
        verify(emailRepository).save(captor.capture());
        EmailConfirmation saved = captor.getValue();
        // Id manual: a entidade nao tem generator, sem ele o persist falha.
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getStatus()).isEqualTo(ConfirmationStatus.PENDENTE);
        assertThat(saved.getExpiresAt()).isNotNull();

        verify(mailer).send("rafael@email.com", "Rafael", CONFIRMATION_URL + "?token=" + token);
    }

    @Test
    @DisplayName("sendConfirmation deve falhar e nao enviar email quando o usuario nao existe")
    void deveFalharUsuarioInexistente() {
        when(repository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendConfirmation("inexistente", "x@email.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuario nao encontrado");

        verify(emailRepository, never()).save(any());
        verificaQueNaoEnviou();
    }

    // ---- resendConfirmation ----

    private EmailConfirmation lastConfirmationSentAt(LocalDateTime sentAt) {
        return EmailConfirmation.builder()
                .id("conf-1")
                .user(userStub())
                .token("token-antigo")
                .status(ConfirmationStatus.PENDENTE)
                .sentAt(sentAt)
                .build();
    }

    @Test
    @DisplayName("resendConfirmation deve enviar novo token quando o cooldown ja passou")
    void deveReenviarForaDoCooldown() {
        when(repository.findById("id-123")).thenReturn(Optional.of(userStub()));
        when(emailRepository.findFirstByUserIdOrderBySentAtDesc("id-123"))
                .thenReturn(Optional.of(lastConfirmationSentAt(LocalDateTime.now().minusSeconds(60))));

        String token = service.resendConfirmation("id-123");

        assertThat(token).isNotBlank();
        assertThat(token).isNotEqualTo("token-antigo");
        verify(emailRepository).save(any(EmailConfirmation.class));
        verify(mailer).send(eq("rafael@email.com"), eq("Rafael"), anyString());
    }

    @Test
    @DisplayName("resendConfirmation deve enviar quando nao ha envio anterior registrado")
    void deveReenviarSemEnvioAnterior() {
        when(repository.findById("id-123")).thenReturn(Optional.of(userStub()));
        when(emailRepository.findFirstByUserIdOrderBySentAtDesc("id-123")).thenReturn(Optional.empty());

        String token = service.resendConfirmation("id-123");

        assertThat(token).isNotBlank();
        verify(mailer).send(eq("rafael@email.com"), eq("Rafael"), anyString());
    }

    @Test
    @DisplayName("resendConfirmation deve bloquear dentro do cooldown de 30s, informando a espera")
    void deveBloquearDentroDoCooldown() {
        when(repository.findById("id-123")).thenReturn(Optional.of(userStub()));
        when(emailRepository.findFirstByUserIdOrderBySentAtDesc("id-123"))
                .thenReturn(Optional.of(lastConfirmationSentAt(LocalDateTime.now().minusSeconds(10))));

        assertThatThrownBy(() -> service.resendConfirmation("id-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aguarde")
                .hasMessageContaining("segundos para reenviar");

        verify(emailRepository, never()).save(any());
        verificaQueNaoEnviou();
    }

    @Test
    @DisplayName("resendConfirmation deve falhar quando a conta ja esta ATIVA")
    void deveFalharReenvioContaAtiva() {
        User active = userStub();
        active.setStatus(RegistrationStatus.ATIVO);
        when(repository.findById("id-123")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.resendConfirmation("id-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ja confirmado");

        verificaQueNaoEnviou();
    }

    @Test
    @DisplayName("resendConfirmation deve falhar quando a conta esta INATIVA (cadastro expirado)")
    void deveFalharReenvioContaInativa() {
        User inactive = userStub();
        inactive.setStatus(RegistrationStatus.INATIVO);
        when(repository.findById("id-123")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.resendConfirmation("id-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cadastro expirado, faca um novo cadastro");

        verificaQueNaoEnviou();
    }
}
