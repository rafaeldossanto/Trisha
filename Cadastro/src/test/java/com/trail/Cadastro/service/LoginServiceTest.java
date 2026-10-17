package com.trail.Cadastro.service;

import com.trail.Cadastro.auth.IssuedToken;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.exception.TooManyAttemptsException;
import com.trail.Cadastro.model.dto.response.AuthenticationResponse;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    // Hash gerado uma unica vez: encode do BCrypt e custoso de proposito.
    private static final String SENHA_HASH = new BCryptPasswordEncoder().encode("senha123");

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    // Encoder real (nao mock): garante que a ordem raw/hash do matches esta certa.
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private LoginService service;

    private User userStub(RegistrationStatus status) {
        return User.builder()
                .id("id-123")
                .name("Rafael")
                .email("rafael@email.com")
                .password(SENHA_HASH)
                .userCode("rafael#1")
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void login_deveRetornarTokenEUsuario_quandoCredenciaisValidasEContaAtiva() {
        User user = userStub(RegistrationStatus.ATIVO);
        when(userRepository.findByEmail("rafael@email.com")).thenReturn(user);
        when(tokenService.issue(user)).thenReturn(new IssuedToken("token-jwt", 7200L));

        AuthenticationResponse response = service.login("rafael@email.com", "senha123");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("token-jwt");
        assertThat(response.user().email()).isEqualTo("rafael@email.com");
    }

    @Test
    void login_deveFalhar_quandoEmailNaoExiste() {
        when(userRepository.findByEmail("inexistente@email.com")).thenReturn(null);

        assertThatThrownBy(() -> service.login("inexistente@email.com", "senha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ou senha invalidos");

        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_deveFalhar_quandoSenhaIncorreta() {
        when(userRepository.findByEmail("rafael@email.com"))
                .thenReturn(userStub(RegistrationStatus.ATIVO));

        assertThatThrownBy(() -> service.login("rafael@email.com", "senha-errada"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ou senha invalidos");

        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_deveFalhar_quandoUsuarioSocialSemSenha() {
        User social = userStub(RegistrationStatus.ATIVO);
        social.setPassword(null);
        when(userRepository.findByEmail("rafael@email.com")).thenReturn(social);

        assertThatThrownBy(() -> service.login("rafael@email.com", "senha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ou senha invalidos");

        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_deveFalhar_quandoContaPendente() {
        when(userRepository.findByEmail("rafael@email.com"))
                .thenReturn(userStub(RegistrationStatus.PENDENTE));

        // Mesma mensagem dos outros casos: dizer "confirme seu email" revelaria
        // que o endereco esta cadastrado, permitindo enumerar a base.
        assertThatThrownBy(() -> service.login("rafael@email.com", "senha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ou senha invalidos");

        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_naoDeveDistinguirEmailInexistenteDeContaPendente() {
        // Regressao anti-enumeracao: as duas respostas tem que ser identicas.
        when(userRepository.findByEmail("naoexiste@email.com")).thenReturn(null);
        when(userRepository.findByEmail("pendente@email.com"))
                .thenReturn(userStub(RegistrationStatus.PENDENTE));

        String inexistente = catchThrowable(
                () -> service.login("naoexiste@email.com", "senha123")).getMessage();
        String pendente = catchThrowable(
                () -> service.login("pendente@email.com", "senha123")).getMessage();

        assertThat(inexistente).isEqualTo(pendente);
    }

    @Test
    void login_deveFalhar_quandoContaInativa() {
        when(userRepository.findByEmail("rafael@email.com"))
                .thenReturn(userStub(RegistrationStatus.INATIVO));

        // Mesma mensagem de credencial invalida: nao revela o estado da conta.
        assertThatThrownBy(() -> service.login("rafael@email.com", "senha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ou senha invalidos");

        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_deveBloquear_quandoContaExcedeuTentativas() {
        doThrow(new TooManyAttemptsException("Muitas tentativas de login. Tente novamente em alguns minutos."))
                .when(loginAttemptService).assertNotBlocked("rafael@email.com");

        assertThatThrownBy(() -> service.login("rafael@email.com", "senha123"))
                .isInstanceOf(TooManyAttemptsException.class);

        // Nem toca o banco nem gera BCrypt/token: a trava corta antes.
        verify(userRepository, never()).findByEmail(any());
        verify(tokenService, never()).issue(any());
    }

    @Test
    void login_deveRegistrarFalha_quandoSenhaIncorreta() {
        when(userRepository.findByEmail("rafael@email.com"))
                .thenReturn(userStub(RegistrationStatus.ATIVO));

        assertThatThrownBy(() -> service.login("rafael@email.com", "senha-errada"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(loginAttemptService).recordFailure("rafael@email.com");
    }

    @Test
    void login_deveZerarContador_quandoLoginBemSucedido() {
        User user = userStub(RegistrationStatus.ATIVO);
        when(userRepository.findByEmail("rafael@email.com")).thenReturn(user);
        when(tokenService.issue(user)).thenReturn(new IssuedToken("token-jwt", 7200L));

        service.login("rafael@email.com", "senha123");

        verify(loginAttemptService).reset("rafael@email.com");
    }
}
