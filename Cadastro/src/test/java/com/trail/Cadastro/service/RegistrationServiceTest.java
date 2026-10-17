package com.trail.Cadastro.service;

import com.trail.Cadastro.model.dto.request.UserCreateRequest;
import com.trail.Cadastro.model.dto.response.UserDTO;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.model.event.UserRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService")
class RegistrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RegistrationService service;

    private UserDTO userDTOStub() {
        return UserDTO.builder()
                .id("id-123")
                .name("Rafael")
                .email("rafael@email.com")
                .userCode("rafael#1")
                .status(RegistrationStatus.PENDENTE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserCreateRequest createRequestStub() {
        return new UserCreateRequest("Rafael", "rafael@email.com", "senha123");
    }

    @Test
    @DisplayName("cria o usuario e publica o evento que dispara o email de confirmacao")
    void register_deveCriarUsuarioEPublicarEvento() {
        when(userService.create(any())).thenReturn(userDTOStub());

        UserDTO result = service.register(createRequestStub());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("id-123");
        verify(userService).create(any(UserCreateRequest.class));

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("id-123");
        assertThat(captor.getValue().email()).isEqualTo("rafael@email.com");
    }

    /**
     * Sem orquestrador externo nao ha mais o que compensar: se a criacao falha, a
     * transacao reverte sozinha e nenhum email e disparado.
     */
    @Test
    @DisplayName("nao publica evento quando a criacao do usuario falha")
    void register_naoDevePublicarEvento_quandoCriacaoFalha() {
        when(userService.create(any()))
                .thenThrow(new IllegalArgumentException("Conta com esse email ja existente"));

        assertThatThrownBy(() -> service.register(createRequestStub()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conta com esse email ja existente");

        verifyNoInteractions(eventPublisher);
    }
}
