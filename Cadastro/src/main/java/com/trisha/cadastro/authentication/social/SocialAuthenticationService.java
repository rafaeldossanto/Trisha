package com.trisha.cadastro.authentication.social;

import com.trisha.cadastro.account.User;
import com.trisha.cadastro.account.UserService;
import com.trisha.cadastro.authentication.AuthenticationMapper;
import com.trisha.cadastro.authentication.dto.AuthenticationResponse;
import com.trisha.cadastro.token.IssuedToken;
import com.trisha.cadastro.token.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class SocialAuthenticationService {

    private final UserService userService;
    private final LinkedAccountRepository linkedAccountRepository;
    private final TokenService tokenService;
    private final Map<AuthProvider, SocialTokenVerifier> verifiers;

    public SocialAuthenticationService(UserService userService,
                                       LinkedAccountRepository linkedAccountRepository,
                                       TokenService tokenService,
                                       List<SocialTokenVerifier> availableVerifiers) {
        this.userService = userService;
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

        user = userService.ensureActive(user);

        IssuedToken token = tokenService.issue(user);
        return AuthenticationMapper.toResponse(user, token);
    }

    private ProviderUserData verify(AuthProvider provider, String idToken) {
        SocialTokenVerifier verifier = verifiers.get(provider);
        if (isNull(verifier)) {
            throw new IllegalArgumentException("Provedor de login nao suportado: " + provider);
        }
        return verifier.verify(idToken);
    }

    private User resolveByEmailOrCreate(AuthProvider provider, ProviderUserData data) {
        User user = Optional.ofNullable(data.email())
                .flatMap(userService::findByEmail)
                .orElseGet(() -> createSocialUser(data));
        link(user, provider, data);
        return user;
    }

    private User createSocialUser(ProviderUserData data) {
        User saved = userService.createActive(nameOrDefault(data), data.email());
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
