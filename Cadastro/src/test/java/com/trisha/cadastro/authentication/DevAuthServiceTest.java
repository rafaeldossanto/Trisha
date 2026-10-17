package com.trisha.cadastro.authentication;

import com.trisha.cadastro.account.RegistrationStatus;
import com.trisha.cadastro.account.User;
import com.trisha.cadastro.account.UserService;
import com.trisha.cadastro.authentication.dto.AuthenticationResponse;
import com.trisha.cadastro.token.IssuedToken;
import com.trisha.cadastro.token.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevAuthService")
class DevAuthServiceTest {

    private static final String EMAIL = "dev@trilha.com";

    @Mock
    private UserService userService;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private DevAuthService service;

    private User userStub() {
        return User.builder()
                .id("id-dev").name("Dev").email(EMAIL)
                .userCode("dev#1").status(RegistrationStatus.ATIVO)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("usuario ja existe -> emite o token sem criar outro")
    void deveLogarUsuarioExistente() {
        User user = userStub();
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.login(EMAIL, "Dev");

        assertThat(response.user().id()).isEqualTo("id-dev");
        assertThat(response.accessToken()).isEqualTo("jwt");
        verify(userService, never()).createActive(any(), any());
    }

    @Test
    @DisplayName("primeiro acesso -> cria o usuario ATIVO pelo UserService e emite o token")
    void deveCriarUsuarioNoPrimeiroAcesso() {
        User created = userStub();
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userService.createActive("Dev", EMAIL)).thenReturn(created);
        when(tokenService.issue(created)).thenReturn(new IssuedToken("jwt", 7200));

        AuthenticationResponse response = service.login(EMAIL, "Dev");

        assertThat(response.user().id()).isEqualTo("id-dev");
        verify(userService).createActive("Dev", EMAIL);
    }
}
