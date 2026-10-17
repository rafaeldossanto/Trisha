package com.trail.Cadastro.service;

import com.trail.Cadastro.auth.ProviderUserData;
import com.trail.Cadastro.auth.IssuedToken;
import com.trail.Cadastro.auth.SocialTokenVerifier;
import com.trail.Cadastro.entity.LinkedAccount;
import com.trail.Cadastro.entity.User;
import com.trail.Cadastro.mapper.AuthenticationMapper;
import com.trail.Cadastro.mapper.UserMapper;
import com.trail.Cadastro.model.dto.response.AuthenticationResponse;
import com.trail.Cadastro.model.enums.AuthProvider;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.repository.LinkedAccountRepository;
import com.trail.Cadastro.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class SocialAuthenticationService {

    private final UserRepository userRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final TokenService tokenService;
    private final Map<AuthProvider, SocialTokenVerifier> verifiers;

    public SocialAuthenticationService(UserRepository userRepository,
                                       LinkedAccountRepository linkedAccountRepository,
                                       TokenService tokenService,
                                       List<SocialTokenVerifier> availableVerifiers) {
        this.userRepository = userRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.tokenService = tokenService;
        this.verifiers = new EnumMap<>(AuthProvider.class);
        availableVerifiers.forEach(v -> this.verifiers.put(v.provider(), v));
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthProvider provider, String idToken) {
        ProviderUserData data = verify(provider, idToken);
        log.info("Login social {} para subject {}", provider, data.subject());

        User user = linkedAccountRepository
                .findByProviderAndProviderUserId(provider, data.subject())
                .map(LinkedAccount::getUser)
                .orElseGet(() -> resolveByEmailOrCreate(provider, data));

        user = ensureActive(user);

        IssuedToken token = tokenService.issue(user);
        return AuthenticationMapper.toResponse(user, token);
    }

    /**
     * Normaliza o status antes de emitir o token — o social nunca autentica uma
     * conta que nao esteja ATIVA:
     * - PENDENTE: o provedor ja confirmou o email, entao a conta e ativada
     *   direto, sem exigir confirmacao nem aceite de termos (mesma convencao do
     *   usuario criado direto pelo social). Sem isso RegistrationCleanupJob
     *   desativaria a conta em uso ao fim do prazo de confirmacao.
     * - INATIVO: conta desativada (prazo de confirmacao estourado ou delecao)
     *   recuperada pelo login social, espelhando o reaproveitamento do create.
     */
    private User ensureActive(User user) {
        if (RegistrationStatus.ATIVO.equals(user.getStatus())) {
            return user;
        }

        user.setStatus(RegistrationStatus.ATIVO);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("Usuario {} ativado no login social", saved.getId());
        return saved;
    }

    private ProviderUserData verify(AuthProvider provider, String idToken) {
        SocialTokenVerifier verifier = verifiers.get(provider);
        if (isNull(verifier)) {
            throw new IllegalArgumentException("Provedor de login nao suportado: " + provider);
        }
        return verifier.verify(idToken);
    }

    private User resolveByEmailOrCreate(AuthProvider provider, ProviderUserData data) {
        User existing = nonNull(data.email()) ? userRepository.findByEmail(data.email()) : null;

        User user = nonNull(existing) ? existing : createSocialUser(data);
        link(user, provider, data);
        return user;
    }

    private User createSocialUser(ProviderUserData data) {
        Long sequence = userRepository.nextSequence();
        User user = UserMapper.toEntitySocial(data, nameOrDefault(data), sequence);
        User saved = userRepository.save(user);
        log.info("Usuario social criado: {} ({})", saved.getUserCode(), saved.getId());
        return saved;
    }

    private void link(User user, AuthProvider provider, ProviderUserData data) {
        LinkedAccount link = LinkedAccount.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .provider(provider)
                .providerUserId(data.subject())
                .email(data.email())
                .linkedAt(LocalDateTime.now())
                .build();
        linkedAccountRepository.save(link);
        log.info("Conta {} vinculada ao usuario {}", provider, user.getId());
    }

    private String nameOrDefault(ProviderUserData data) {
        if (nonNull(data.name()) && !data.name().isBlank()) {
            return data.name();
        }
        if (nonNull(data.email()) && data.email().contains("@")) {
            return data.email().substring(0, data.email().indexOf("@"));
        }
        return "usuario";
    }
}
