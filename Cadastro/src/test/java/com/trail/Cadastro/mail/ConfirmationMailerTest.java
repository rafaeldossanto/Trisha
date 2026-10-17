package com.trail.Cadastro.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmationMailer")
class ConfirmationMailerTest {

    private static final String LINK = "http://localhost:8080/auth/confirmar-email?token=token-abc";

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private ConfirmationMailer mailer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailer, "sender", "nao-responda@trilha.com");
    }

    @Test
    @DisplayName("entrega a mensagem no JavaMailSender")
    void deveEnviar() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        mailer.send("rafael@email.com", "Rafael", LINK);

        verify(mailSender).send(any(MimeMessage.class));
    }

    /**
     * A falha precisa virar IllegalStateException porque e esse o tipo que
     * @Retryable observa para reentregar.
     */
    @Test
    @DisplayName("converte falha do SMTP em IllegalStateException (o gatilho do retry)")
    void deveConverterFalhaDeEnvio() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("smtp fora do ar")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> mailer.send("rafael@email.com", "Rafael", LINK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nao foi possivel enviar o email de confirmacao");
    }
}
