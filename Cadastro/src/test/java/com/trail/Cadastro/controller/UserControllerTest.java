package com.trail.Cadastro.controller;

import com.trail.Cadastro.exception.ForbiddenException;
import com.trail.Cadastro.model.dto.request.UserCreateRequest;
import com.trail.Cadastro.model.dto.request.UserUpdateRequest;
import com.trail.Cadastro.model.dto.response.UserDTO;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.service.RegistrationService;
import com.trail.Cadastro.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String USER_ID = "id-123";

    @Mock
    private UserService userService;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private UserController controller;

    /** Token cujo subject e o id interno da conta — o que o ensureSelf compara. */
    private Jwt tokenOf(String userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId)
                .build();
    }

    private UserDTO userDTOStub() {
        return UserDTO.builder()
                .id(USER_ID)
                .name("Rafael")
                .email("rafael@email.com")
                .userCode("rafael#1")
                .status(RegistrationStatus.PENDENTE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_deveRetornarDTO_quandoSucesso() {
        when(registrationService.register(any())).thenReturn(userDTOStub());

        UserDTO result = controller.create(
                new UserCreateRequest("Rafael", "rafael@email.com", "senha123")
        );

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("rafael@email.com");
        verify(registrationService).register(any(UserCreateRequest.class));
    }

    @Test
    void getById_deveRetornarDTO_quandoUsuarioExiste() {
        when(userService.getById(USER_ID)).thenReturn(userDTOStub());

        UserDTO result = controller.getById(tokenOf(USER_ID), USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Rafael");
    }

    @Test
    void update_deveRetornarDTO_quandoSucesso() {
        when(userService.update(any(), eq(USER_ID))).thenReturn(userDTOStub());

        UserDTO result = controller.update(
                tokenOf(USER_ID), new UserUpdateRequest("Rafael Atualizado", null), USER_ID
        );

        assertThat(result).isNotNull();
        verify(userService).update(any(), eq(USER_ID));
    }

    @Test
    void delete_deveChamarService_quandoSucesso() {
        doNothing().when(userService).delete(USER_ID);

        controller.delete(tokenOf(USER_ID), USER_ID);

        verify(userService).delete(USER_ID);
    }

    // --- IDOR: o token de um usuario nao opera a conta de outro -------------

    @Test
    void getById_deveRecusar_quandoTokenEDeOutroUsuario() {
        assertThatThrownBy(() -> controller.getById(tokenOf("outro-id"), USER_ID))
                .isInstanceOf(ForbiddenException.class);

        // Recusa antes de consultar: nao revela se o id alvo existe.
        verifyNoInteractions(userService);
    }

    @Test
    void update_deveRecusar_quandoTokenEDeOutroUsuario() {
        assertThatThrownBy(() -> controller.update(
                tokenOf("outro-id"), new UserUpdateRequest("Invasor", null), USER_ID))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void delete_deveRecusar_quandoTokenEDeOutroUsuario() {
        assertThatThrownBy(() -> controller.delete(tokenOf("outro-id"), USER_ID))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void getById_deveRecusar_quandoSemToken() {
        assertThatThrownBy(() -> controller.getById(null, USER_ID))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(userService);
    }
}
