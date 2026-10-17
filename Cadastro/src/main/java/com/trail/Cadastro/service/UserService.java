package com.trail.Cadastro.service;

import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.mapper.UserMapper;
import com.trail.Cadastro.model.dto.request.UserCreateRequest;
import com.trail.Cadastro.model.dto.request.UserUpdateRequest;
import com.trail.Cadastro.model.dto.response.UserDTO;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserDTO create(UserCreateRequest request) {
        log.info("Iniciando processo de cadastro de usuario");
        User find = repository.findByEmail(request.email());
        if (nonNull(find)) {
            if (!RegistrationStatus.INATIVO.equals(find.getStatus())) {
                throw new IllegalArgumentException("Conta com esse email ja existente");
            }
            return reactivate(find, request);
        }

        Long sequence = repository.nextSequence();
        User user = UserMapper.toEntity(request, passwordEncoder.encode(request.password()), sequence);
        repository.save(user);
        log.info("Processo finalizado com sucesso");
        return UserMapper.toResponse(user);
    }

    private UserDTO reactivate(User user, UserCreateRequest request) {
        User reactivated = UserMapper.mapToReactivated(user, request, passwordEncoder.encode(request.password()));
        repository.save(reactivated);
        log.info("Cadastro reativado para conta inativa");
        return UserMapper.toResponse(reactivated);
    }

    public UserDTO update(UserUpdateRequest request, String id) {
        log.info("Iniciando processo de atualizacao de usuario");
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        if (isNull(request)) throw new IllegalArgumentException("O campo precisa ser preenchido");

        if (nonNull(request.name())) {
            user.setName(request.name());
        }

        if (nonNull(request.email())) {
            User existing = repository.findByEmail(request.email());
            if (nonNull(existing) && !existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Conta com esse email ja existente");
            }
            user.setEmail(request.email());
        }

        log.info("Usuario atualizado com sucesso");
        return UserMapper.toResponse(repository.save(user));
    }

    public UserDTO getById(String id) {
        log.info("Iniciando processo de consulta de usuario");
        return UserMapper.toResponse(
                repository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"))
        );
    }

    public void delete(String id) {
        log.info("Iniciando processo de desativacao de usuario");
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        user.setStatus(RegistrationStatus.INATIVO);
        repository.save(user);
        log.info("Delecao de usuario bem sucedida");
    }
}
