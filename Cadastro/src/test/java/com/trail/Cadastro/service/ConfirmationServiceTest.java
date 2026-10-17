package com.trail.Cadastro.service;

import com.trail.Cadastro.entity.EmailConfirmation;
import com.trail.Cadastro.entity.TermsAcceptance;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.model.enums.ConfirmationStatus;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.EmailConfirmationRepository;
import com.trail.Cadastro.repository.TermsAcceptanceRepository;
import com.trail.Cadastro.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmationService")
class ConfirmationServiceTest {

    @Mock
    private EmailConfirmationRepository confirmationRepository;
    @Mock
    private TermsAcceptanceRepository termsRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConfirmationService service;

    private User userStub() {
        return User.builder()
                .id("id-123")
                .name("Rafael")
                .email("rafael@email.com")
                .build();
    }

    private EmailConfirmation confirmationStub(ConfirmationStatus status, LocalDateTime expiresAt) {
        return EmailConfirmation.builder()
                .id("conf-1")
                .user(userStub())
                .token("token-abc")
                .status(status)
                .expiresAt(expiresAt)
                .build();
    }

    private void emailConfirmado(boolean confirmado) {
        when(confirmationRepository.existsByUserIdAndStatus("id-123", ConfirmationStatus.CONFIRMADO))
                .thenReturn(confirmado);
    }

    private void termosAceitos(boolean aceitos) {
        when(termsRepository.existsByUserIdAndAcceptedTrue("id-123")).thenReturn(aceitos);
    }

    private void verificaQueNaoAtivou() {
        verify(userRepository, never()).activateIfPending(any(), any());
    }

    // ---- confirmEmail ----

    @Nested
    @DisplayName("confirmEmail")
    class ConfirmEmail {

        @Test
        @DisplayName("grava CONFIRMADO e ativa a conta quando os termos ja tinham sido aceitos")
        void deveConfirmarEAtivar() {
            EmailConfirmation confirmation =
                    confirmationStub(ConfirmationStatus.PENDENTE, LocalDateTime.now().plusMinutes(5));
            when(confirmationRepository.findByToken("token-abc")).thenReturn(Optional.of(confirmation));
            emailConfirmado(true);
            termosAceitos(true);
            when(userRepository.activateIfPending(eq("id-123"), any())).thenReturn(1);

            service.confirmEmail("token-abc");

            assertThat(confirmation.getStatus()).isEqualTo(ConfirmationStatus.CONFIRMADO);
            assertThat(confirmation.getConfirmedAt()).isNotNull();
            verify(confirmationRepository).save(confirmation);
            verify(userRepository).activateIfPending(eq("id-123"), any());
        }

        @Test
        @DisplayName("grava CONFIRMADO mas NAO ativa enquanto os termos nao forem aceitos")
        void naoDeveAtivarSemTermos() {
            EmailConfirmation confirmation =
                    confirmationStub(ConfirmationStatus.PENDENTE, LocalDateTime.now().plusMinutes(5));
            when(confirmationRepository.findByToken("token-abc")).thenReturn(Optional.of(confirmation));
            emailConfirmado(true);
            termosAceitos(false);

            service.confirmEmail("token-abc");

            assertThat(confirmation.getStatus()).isEqualTo(ConfirmationStatus.CONFIRMADO);
            verify(confirmationRepository).save(confirmation);
            verificaQueNaoAtivou();
        }

        @Test
        @DisplayName("falha quando o token nao existe")
        void deveFalharTokenInvalido() {
            when(confirmationRepository.findByToken("token-x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirmEmail("token-x"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token invalido");

            verificaQueNaoAtivou();
        }

        @Test
        @DisplayName("falha sem reativar quando ja confirmado")
        void deveFalharJaConfirmado() {
            EmailConfirmation confirmation =
                    confirmationStub(ConfirmationStatus.CONFIRMADO, LocalDateTime.now().plusMinutes(5));
            when(confirmationRepository.findByToken("token-abc")).thenReturn(Optional.of(confirmation));

            assertThatThrownBy(() -> service.confirmEmail("token-abc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email ja confirmado");

            verify(confirmationRepository, never()).save(any());
            verificaQueNaoAtivou();
        }

        @Test
        @DisplayName("marca EXPIRADO e falha quando o token venceu")
        void deveFalharTokenExpirado() {
            EmailConfirmation confirmation =
                    confirmationStub(ConfirmationStatus.PENDENTE, LocalDateTime.now().minusMinutes(1));
            when(confirmationRepository.findByToken("token-abc")).thenReturn(Optional.of(confirmation));

            assertThatThrownBy(() -> service.confirmEmail("token-abc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token expirado");

            assertThat(confirmation.getStatus()).isEqualTo(ConfirmationStatus.EXPIRADO);
            verify(confirmationRepository).save(confirmation);
            verificaQueNaoAtivou();
        }

        @Test
        @DisplayName("falha quando o dono do token ja esta ATIVO (confirmou por outro caminho)")
        void deveFalharDonoJaAtivo() {
            EmailConfirmation confirmation =
                    confirmationStub(ConfirmationStatus.PENDENTE, LocalDateTime.now().plusMinutes(5));
            confirmation.getUser().setStatus(RegistrationStatus.ATIVO);
            when(confirmationRepository.findByToken("token-abc")).thenReturn(Optional.of(confirmation));

            assertThatThrownBy(() -> service.confirmEmail("token-abc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email ja confirmado");

            verify(confirmationRepository, never()).save(any());
            verificaQueNaoAtivou();
        }

        @Test
        @DisplayName("falha quando a conta ja expirou (token reenviado tardio)")
        void deveFalharDonoInativo() {
            EmailConfirmation confirmation =
                    confirmationStub(ConfirmationStatus.PENDENTE, LocalDateTime.now().plusMinutes(5));
            confirmation.getUser().setStatus(RegistrationStatus.INATIVO);
            when(confirmationRepository.findByToken("token-abc")).thenReturn(Optional.of(confirmation));

            assertThatThrownBy(() -> service.confirmEmail("token-abc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cadastro expirado, faca um novo cadastro");

            verify(confirmationRepository, never()).save(any());
            verificaQueNaoAtivou();
        }
    }

    // ---- acceptTerms ----

    @Nested
    @DisplayName("acceptTerms")
    class AcceptTerms {

        @Test
        @DisplayName("grava o aceite e ativa a conta quando o email ja estava confirmado")
        void deveAceitarEAtivar() {
            when(userRepository.findById("id-123")).thenReturn(Optional.of(userStub()));
            emailConfirmado(true);
            termosAceitos(true);
            when(userRepository.activateIfPending(eq("id-123"), any())).thenReturn(1);

            service.acceptTerms("id-123");

            ArgumentCaptor<TermsAcceptance> captor = ArgumentCaptor.forClass(TermsAcceptance.class);
            verify(termsRepository).save(captor.capture());
            TermsAcceptance saved = captor.getValue();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getAccepted()).isTrue();
            assertThat(saved.getAcceptedAt()).isNotNull();

            verify(userRepository).activateIfPending(eq("id-123"), any());
        }

        @Test
        @DisplayName("grava o aceite mas NAO ativa enquanto o email nao for confirmado")
        void naoDeveAtivarSemEmailConfirmado() {
            when(userRepository.findById("id-123")).thenReturn(Optional.of(userStub()));
            emailConfirmado(false);

            service.acceptTerms("id-123");

            verify(termsRepository).save(any(TermsAcceptance.class));
            verificaQueNaoAtivou();
        }

        @Test
        @DisplayName("falha sem gravar quando o usuario nao existe")
        void deveFalharAceiteUsuarioInexistente() {
            when(userRepository.findById("inexistente")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.acceptTerms("inexistente"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Usuario nao encontrado");

            verify(termsRepository, never()).save(any());
            verificaQueNaoAtivou();
        }
    }

    // ---- concorrencia / idempotencia ----

    @Nested
    @DisplayName("ativacao concorrente")
    class AtivacaoConcorrente {

        /**
         * Confirmar email e aceitar termos podem chegar simultaneos — o link do
         * email costuma ser aberto em outro dispositivo, entao a ordem das telas
         * do front nao impede a corrida. Quem arbitra e o UPDATE condicional: a
         * segunda chamada afeta zero linhas e nada acontece duas vezes.
         */
        @Test
        @DisplayName("a segunda ativacao afeta zero linhas e nao lanca")
        void deveSerIdempotente() {
            when(userRepository.findById("id-123")).thenReturn(Optional.of(userStub()));
            emailConfirmado(true);
            termosAceitos(true);
            when(userRepository.activateIfPending(eq("id-123"), any())).thenReturn(0);

            service.acceptTerms("id-123");

            verify(userRepository).activateIfPending(eq("id-123"), any());
        }
    }
}
