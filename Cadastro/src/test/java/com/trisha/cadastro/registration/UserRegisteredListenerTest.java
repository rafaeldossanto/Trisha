package com.trisha.cadastro.registration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegisteredListener")
class UserRegisteredListenerTest {

    @Mock
    private ConfirmationEmailService confirmationEmailService;

    @InjectMocks
    private UserRegisteredListener listener;

    @Test
    @DisplayName("dispara o envio do email de confirmacao para o usuario do evento")
    void deveEnviarEmailDeConfirmacao() {
        listener.onUserRegistered(new UserRegisteredEvent("id-123", "rafael@email.com"));

        verify(confirmationEmailService).sendConfirmation("id-123", "rafael@email.com");
    }

    /**
     * Roda depois do commit e fora da thread da requisicao: a resposta HTTP ja
     * foi enviada, entao propagar a excecao daqui nao alcancaria ninguem. A conta
     * fica PENDENTE e RegistrationCleanupJob a recolhe ao fim do prazo.
     */
    @Test
    @DisplayName("engole a falha definitiva de envio em vez de propagar")
    void naoDevePropagarFalhaDeEnvio() {
        when(confirmationEmailService.sendConfirmation("id-123", "rafael@email.com"))
                .thenThrow(new IllegalStateException("Nao foi possivel enviar o email de confirmacao"));

        assertThatCode(() -> listener.onUserRegistered(new UserRegisteredEvent("id-123", "rafael@email.com")))
                .doesNotThrowAnyException();
    }
}
