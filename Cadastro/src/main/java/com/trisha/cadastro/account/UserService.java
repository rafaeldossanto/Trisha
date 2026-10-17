package com.trisha.cadastro.account;

import com.trisha.cadastro.account.dto.UserCreateRequest;
import com.trisha.cadastro.account.dto.UserResponse;
import com.trisha.cadastro.account.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Dono da tabela {@code usuario}: e o unico que cria usuarios e muda o status
 * deles. As outras areas leem pelo repositorio quando precisam, mas escrevem
 * sempre por aqui.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(UserCreateRequest request) {
        log.info("Iniciando processo de cadastro de usuario");
        Optional<User> found = repository.findByEmail(request.email());
        if (found.isPresent()) {
            if (!RegistrationStatus.INATIVO.equals(found.get().getStatus())) {
                throw new IllegalArgumentException("Conta com esse email ja existente");
            }
            return reactivate(found.get(), request);
        }

        Long sequence = repository.nextSequence();
        User user = UserMapper.toEntity(request.name(), request.email(),
                passwordEncoder.encode(request.password()), RegistrationStatus.PENDENTE, sequence);
        repository.save(user);
        log.info("Processo finalizado com sucesso");
        return UserMapper.toResponse(user);
    }

    private UserResponse reactivate(User user, UserCreateRequest request) {
        user.reopen(request.name(), passwordEncoder.encode(request.password()));
        repository.save(user);
        log.info("Cadastro reativado para conta inativa");
        return UserMapper.toResponse(user);
    }

    /**
     * Usuario que ja nasce ATIVO e sem senha: login social (o provedor ja
     * confirmou o email) e dev-login.
     */
    public User createActive(String name, String email) {
        Long sequence = repository.nextSequence();
        return repository.save(UserMapper.toEntity(name, email, null, RegistrationStatus.ATIVO, sequence));
    }

    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    /**
     * Normaliza o status antes de o login social emitir o token — o social
     * nunca autentica uma conta que nao esteja ATIVA:
     * - PENDENTE: o provedor ja confirmou o email, entao a conta e ativada
     *   direto, sem exigir confirmacao nem aceite de termos (mesma convencao do
     *   usuario criado direto pelo social). Sem isso RegistrationCleanupJob
     *   desativaria a conta em uso ao fim do prazo de confirmacao.
     * - INATIVO: conta desativada (prazo de confirmacao estourado ou delecao)
     *   recuperada pelo login social, espelhando o reaproveitamento do create.
     */
    public User ensureActive(User user) {
        if (user.isActive()) {
            return user;
        }

        user.activate(LocalDateTime.now());
        User saved = repository.save(user);
        log.info("Usuario {} ativado no login social", saved.getId());
        return saved;
    }

    /**
     * Fecha o cadastro por senha (email confirmado e termos aceitos) num UPDATE
     * condicional; devolve quantas linhas foram afetadas (0 ou 1). Ver
     * {@link UserRepository#activateIfPending}.
     */
    public int activateIfPending(String userId) {
        return repository.activateIfPending(userId, LocalDateTime.now());
    }

    public UserResponse update(UserUpdateRequest request, String id) {
        log.info("Iniciando processo de atualizacao de usuario");
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        if (isNull(request)) throw new IllegalArgumentException("O campo precisa ser preenchido");

        if (nonNull(request.name())) {
            user.setName(request.name());
        }

        if (nonNull(request.email())) {
            boolean takenByOther = repository.findByEmail(request.email())
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .isPresent();
            if (takenByOther) {
                throw new IllegalArgumentException("Conta com esse email ja existente");
            }
            user.setEmail(request.email());
        }

        log.info("Usuario atualizado com sucesso");
        return UserMapper.toResponse(repository.save(user));
    }

    public UserResponse getById(String id) {
        log.info("Iniciando processo de consulta de usuario");
        return UserMapper.toResponse(
                repository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"))
        );
    }

    /**
     * Desativa (status INATIVO), nao apaga a linha. Chamado pela rota
     * {@code DELETE /usuario/{id}} e pelo RegistrationCleanupJob.
     */
    public void deactivate(String id) {
        log.info("Iniciando processo de desativacao de usuario");
        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        user.deactivate();
        repository.save(user);
        log.info("Delecao de usuario bem sucedida");
    }
}
