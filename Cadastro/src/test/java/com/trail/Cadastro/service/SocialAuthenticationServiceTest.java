package com.trail.Cadastro.service;

import com.trail.Cadastro.auth.ProviderUserData;
import com.trail.Cadastro.auth.IssuedToken;
import com.trail.Cadastro.auth.SocialTokenVerifier;
import com.trail.Cadastro.entity.LinkedAccount;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.model.dto.response.AuthenticationResponse;
import com.trail.Cadastro.model.enums.AuthProvider;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.LinkedAccountRepository;
import com.trail.Cadastro.repository.UserRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialAuthenticationService")
class SocialAuthenticationServiceTest {

    private static final String ID_TOKEN = "token-jwt";
    private static final String SUB = "google-sub-123";
    private static final String EMAIL = "rafael@gmail.com";

    @Mock
    private UserRepository userRepository;
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
                userRepository, linkedAccountRepository, tokenService, List.of(googleVerifier));
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

    @Test
    @DisplayName("cenario 1: vinculo ja existe -> loga o usuario vinculado, sem criar nem vincular de novo")
    void deveLogarQuandoVinculoExiste() {
        User user = existingUser();
        LinkedAccount link = LinkedAccount.builder().user(user).build();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.of(link));
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().id()).isEqualTo("usuario-1");
        assertThat(response.accessToken()).isEqualTo("jwt");
        verify(userRepository, never()).save(any());
        verify(linkedAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("cenario 2: email ja existe sem vinculo -> vincula o provedor a conta existente, sem criar usuario")
    void deveVincularAContaExistente() {
        User user = existingUser();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(user);
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().id()).isEqualTo("usuario-1");
        verify(userRepository, never()).save(any());
        verify(linkedAccountRepository).save(any(LinkedAccount.class));
    }

    @Test
    @DisplayName("cenario 3: usuario novo -> cria com userCode e status ATIVO, e vincula o provedor")
    void deveCriarUsuarioNovo() {
        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(null);
        when(userRepository.nextSequence()).thenReturn(7L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();

        assertThat(created.getUserCode()).isEqualTo("rafael#7");
        assertThat(created.getStatus()).isEqualTo(RegistrationStatus.ATIVO);
        assertThat(created.getPassword()).isNull();
        assertThat(response.user().email()).isEqualTo(EMAIL);
        verify(linkedAccountRepository).save(any(LinkedAccount.class));
    }

    @Test
    @DisplayName("conta PENDENTE -> ativa direto antes de logar (o provedor ja confirmou o email)")
    void deveAtivarContaPendente() {
        User pending = existingUser();
        pending.setStatus(RegistrationStatus.PENDENTE);

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(pending);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().id()).isEqualTo("usuario-1");
        assertThat(pending.getStatus()).isEqualTo(RegistrationStatus.ATIVO);
        verify(userRepository).save(pending);
    }

    @Test
    @DisplayName("conta INATIVA com vinculo -> reativa como ATIVO")
    void deveReativarContaInativa() {
        User inactive = existingUser();
        inactive.setStatus(RegistrationStatus.INATIVO);
        LinkedAccount link = LinkedAccount.builder().user(inactive).build();

        when(googleVerifier.verify(ID_TOKEN)).thenReturn(data());
        when(linkedAccountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, SUB))
                .thenReturn(Optional.of(link));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue(any(User.class))).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.authenticate(AuthProvider.GOOGLE, ID_TOKEN);

        assertThat(response.user().id()).isEqualTo("usuario-1");
        assertThat(inactive.getStatus()).isEqualTo(RegistrationStatus.ATIVO);
        verify(userRepository).save(inactive);
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

        verify(userRepository, never()).save(any());
    }
}
