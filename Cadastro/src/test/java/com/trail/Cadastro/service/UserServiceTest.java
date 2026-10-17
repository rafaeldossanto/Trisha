package com.trail.Cadastro.service;

import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.model.dto.request.UserCreateRequest;
import com.trail.Cadastro.model.dto.request.UserUpdateRequest;
import com.trail.Cadastro.model.dto.response.UserDTO;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    // Encoder real (nao mock): valida que a senha persistida e um hash BCrypt.
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService service;

    // ---- stubs reutilizaveis ----

    private User userStub() {
        return User.builder()
                .id("id-123")
                .name("Rafael")
                .email("rafael@email.com")
                .password("senha123")
                .userCode("rafael#1")
                .status(RegistrationStatus.PENDENTE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserCreateRequest createRequestStub() {
        return new UserCreateRequest("Rafael", "rafael@email.com", "senha123");
    }

    // ---- create ----

    @Test
    void create_deveSalvarERetornarDTO_quandoEmailNaoExiste() {
        when(repository.findByEmail("rafael@email.com")).thenReturn(null);
        when(repository.nextSequence()).thenReturn(1L);
        when(repository.save(any())).thenReturn(userStub());

        UserDTO result = service.create(createRequestStub());

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("rafael@email.com");
        assertThat(result.name()).isEqualTo("Rafael");
        verify(repository).save(any(User.class));
    }

    @Test
    void create_deveSalvarSenhaComHash_naoTextoPlano() {
        when(repository.findByEmail("rafael@email.com")).thenReturn(null);
        when(repository.nextSequence()).thenReturn(1L);

        service.create(createRequestStub());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        String savedPassword = captor.getValue().getPassword();
        assertThat(savedPassword).isNotEqualTo("senha123");
        assertThat(passwordEncoder.matches("senha123", savedPassword)).isTrue();
    }

    @Test
    void create_deveLancarExcecao_quandoEmailJaExiste() {
        when(repository.findByEmail("rafael@email.com")).thenReturn(userStub());

        assertThatThrownBy(() -> service.create(createRequestStub()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conta com esse email ja existente");
    }

    @Test
    void create_deveReaproveitarRegistro_quandoEmailPertenceAContaInativa() {
        User inactive = userStub();
        inactive.setStatus(RegistrationStatus.INATIVO);
        when(repository.findByEmail("rafael@email.com")).thenReturn(inactive);

        UserDTO result = service.create(
                new UserCreateRequest("Rafael Novo", "rafael@email.com", "novaSenha"));

        assertThat(result).isNotNull();
        assertThat(inactive.getStatus()).isEqualTo(RegistrationStatus.PENDENTE);
        assertThat(inactive.getName()).isEqualTo("Rafael Novo");
        // A senha nova tambem entra com hash, nunca em texto plano.
        assertThat(inactive.getPassword()).isNotEqualTo("novaSenha");
        assertThat(passwordEncoder.matches("novaSenha", inactive.getPassword())).isTrue();
        // Preserva id e codigoUsuario do registro original.
        assertThat(result.id()).isEqualTo("id-123");
        assertThat(result.userCode()).isEqualTo("rafael#1");
        verify(repository, never()).nextSequence();
        verify(repository).save(inactive);
    }

    // ---- getById ----

    @Test
    void getById_deveRetornarDTO_quandoUsuarioExiste() {
        when(repository.findById("id-123")).thenReturn(Optional.of(userStub()));

        UserDTO result = service.getById("id-123");

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("rafael@email.com");
    }

    @Test
    void getById_deveLancarExcecao_quandoUsuarioNaoExiste() {
        when(repository.findById("id-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("id-inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario nao encontrado");
    }

    // ---- update ----

    @Test
    void update_deveAtualizarNome_quandoEmailNulo() {
        User user = userStub();
        when(repository.findById("id-123")).thenReturn(Optional.of(user));
        when(repository.save(any())).thenReturn(user);

        UserUpdateRequest request = new UserUpdateRequest("Rafael Novo", null);
        UserDTO result = service.update(request, "id-123");

        assertThat(result).isNotNull();
        verify(repository).save(any(User.class));
    }

    @Test
    void update_deveAtualizarEmail_quandoNomeNulo() {
        User user = userStub();
        when(repository.findById("id-123")).thenReturn(Optional.of(user));
        when(repository.findByEmail("novo@email.com")).thenReturn(null);
        when(repository.save(any())).thenReturn(user);

        UserUpdateRequest request = new UserUpdateRequest(null, "novo@email.com");
        UserDTO result = service.update(request, "id-123");

        assertThat(result).isNotNull();
        verify(repository).save(any(User.class));
    }

    @Test
    void update_deveAtualizarNomeEEmail_quandoAmbosPreenchidos() {
        User user = userStub();
        when(repository.findById("id-123")).thenReturn(Optional.of(user));
        when(repository.findByEmail("novo@email.com")).thenReturn(null);
        when(repository.save(any())).thenReturn(user);

        UserUpdateRequest request = new UserUpdateRequest("Rafael Novo", "novo@email.com");
        service.update(request, "id-123");

        assertThat(user.getName()).isEqualTo("Rafael Novo");
        assertThat(user.getEmail()).isEqualTo("novo@email.com");
        verify(repository).save(any(User.class));
    }

    @Test
    void update_deveFalhar_quandoEmailJaPertenceAOutroUsuario() {
        User user = userStub();
        User other = userStub();
        other.setId("outro-id");
        when(repository.findById("id-123")).thenReturn(Optional.of(user));
        when(repository.findByEmail("ocupado@email.com")).thenReturn(other);

        assertThatThrownBy(() -> service.update(new UserUpdateRequest(null, "ocupado@email.com"), "id-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja existente");

        verify(repository, never()).save(any());
    }

    @Test
    void update_deveLancarExcecao_quandoUsuarioNaoExiste() {
        when(repository.findById("id-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new UserUpdateRequest("nome", null), "id-inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario nao encontrado");
    }

    // ---- delete ----

    @Test
    void delete_deveInativarUsuario_quandoExiste() {
        User user = userStub();
        when(repository.findById("id-123")).thenReturn(Optional.of(user));
        when(repository.save(any())).thenReturn(user);

        service.delete("id-123");

        assertThat(user.getStatus()).isEqualTo(RegistrationStatus.INATIVO);
        verify(repository).save(user);
    }

    @Test
    void delete_deveLancarExcecao_quandoUsuarioNaoExiste() {
        when(repository.findById("id-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("id-inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario nao encontrado");
    }
}
