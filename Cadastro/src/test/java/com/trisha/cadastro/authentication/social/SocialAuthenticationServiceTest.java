package com.trisha.cadastro.authentication.social;

import com.trisha.cadastro.account.RegistrationStatus;
import com.trisha.cadastro.account.User;
import com.trisha.cadastro.account.UserService;
import com.trisha.cadastro.authentication.dto.AuthenticationResponse;
import com.trisha.cadastro.token.IssuedToken;
import com.trisha.cadastro.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialAuthenticationService")
class SocialAuthenticationServiceTest {

    private static final String ID_TOKEN = "token-jwt";
    private static final String SUB = "google-sub-123";
    private static final String EMAIL = "rafael@gmail.com";

    @Mock
    private UserService userService;
    @Mock
    private LinkedAccountRepository linkedAccountRepository;
    @Mock
    private SocialTokenVerifier googleVerifier;
    @Mock
    private TokenService tokenService;

    private SocialAuthenticationService service;

    @BeforeEach
    void setUp() {
        when(googleVerifier.provider()).thenReturn(AuthProvider.GOOGLE);
        service = new SocialAuthenticationService(
                userService, linkedAccountRepository, tokenService, List.of(googleVerifier));
    }

    private ProviderUserData data() {
        return new ProviderUserData(SUB, EMAIL, "Rafael");
    }

    private User existingUser() {
        return User.builder()
                .id("usuario-1").name("Rafael").email(EMAIL)
                .userCode("rafael#1").status(RegistrationStatus.ATIVO)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    /** O status e normalizado pelo UserService; aqui so importa que o login passe por ele. */
    private void ensureActiveDevolveOProprioUsuario() {
        when(userService.ensureActive(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("cenario 1: vinculo ja existe -> loga o usuario vinculado, sem criar nem vincular de novo")
    void deveLogarQuandoVinculoExiste() {
        User user = existingUser();
        LinkedAccount link = LinkedAccount.builder().user(user).build();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.of(link));
        ensureActiveDevolveOProprioUsuario();
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().id()).isEqualTo("usuario-1");
        assertThat(response.accessToken()).isEqualTo("jwt");
        verify(userService, never()).createActive(any(), any());
        verify(linkedAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("cenario 2: email ja existe sem vinculo -> vincula o provedor a conta existente, sem criar usuario")
    void deveVincularAContaExistente() {
        User user = existingUser();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        ensureActiveDevolveOProprioUsuario();
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().id()).isEqualTo("usuario-1");
        verify(userService, never()).createActive(any(), any());
        verify(linkedAccountRepository).save(any(LinkedAccount.class));
    }

    @Test
    @DisplayName("cenario 3: usuario novo -> cria pelo UserService (ATIVO) e vincula o provedor")
    void deveCriarUsuarioNovo() {
        User created = existingUser();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userService.createActive("Rafael", EMAIL)).thenReturn(created);
        ensureActiveDevolveOProprioUsuario();
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().email()).isEqualTo(EMAIL);
        ArgumentCaptor<LinkedAccount> captor = ArgumentCaptor.forClass(LinkedAccount.class);
        verify(linkedAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(created);
        assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(captor.getValue().getProviderUserId()).isEqualTo(SUB);
    }

    @Test
    @DisplayName("usuario novo sem nome no token -> usa o comeco do email como nome")
    void deveUsarEmailComoNomeQuandoProvedorNaoMandaNome() {
        when(googleVerifier.verify(ID_TOKEN)).thenReturn(new ProviderUserData(SUB, EMAIL, null));
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userService.createActive("rafael", EMAIL)).thenReturn(existingUser());
        ensureActiveDevolveOProprioUsuario();
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        verify(userService).createActive("rafael", EMAIL);
    }

    @Test
    @DisplayName("conta PENDENTE -> passa pelo ensureActive antes de logar (o provedor ja confirmou o email)")
    void deveAtivarContaPendente() {
        User pending = existingUser();
        pending.setStatus(RegistrationStatus.PENDENTE);
        User activated = existingUser();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(pending));
        when(userService.ensureActive(pending)).thenReturn(activated);
        when(tokenService.issue(activated)).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().status()).isEqualTo(RegistrationStatus.ATIVO);
        verify(userService).ensureActive(pending);
    }

    @Test
    @DisplayName("conta INATIVA com vinculo -> passa pelo ensureActive e loga reativada")
    void deveReativarContaInativa() {
        User inactive = existingUser();
        inactive.setStatus(RegistrationStatus.INATIVO);
        LinkedAccount link = LinkedAccount.builder().user(inactive).build();
        User activated = existingUser();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.of(link));
        when(userService.ensureActive(inactive)).thenReturn(activated);
        when(tokenService.issue(activated)).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().status()).isEqualTo(RegistrationStatus.ATIVO);
        verify(userService).ensureActive(inactive);
    }

    @Test
    @DisplayName("deve falhar quando o provedor nao tem verificador registrado")
    void deveFalharProvedorNaoSuportado() {
        assertThatThrownBy(() -> service.authenticate(AuthProvider.APPLE, ID_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao suportado");
    }

    @Test
    @DisplayName("deve propagar erro quando o token e invalido")
    void devePropagarTokenInvalido() {
        when(googleVerifier.verify(ID_TOKEN))
                .thenThrow(new IllegalArgumentException("Token do Google invalido"));

        assertThatThrownBy(() -> service.authenticate(AuthProvider.GOOGLE, ID_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalido");

        verifyNoInteractions(userService);
    }
}
