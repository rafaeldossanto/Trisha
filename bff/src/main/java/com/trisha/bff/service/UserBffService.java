package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.client.CadastroClient;
import com.trisha.bff.model.dto.request.DevLoginRequest;
import com.trisha.bff.model.dto.request.LoginRequest;
import com.trisha.bff.model.dto.request.SocialLoginRequest;
import com.trisha.bff.model.dto.request.UserCreateRequest;
import com.trisha.bff.model.dto.request.UserUpdateRequest;
import com.trisha.bff.model.dto.response.AuthenticationResponse;
import com.trisha.bff.model.dto.response.PublicUserResponse;
import com.trisha.bff.model.dto.response.UserResponse;
import com.trisha.bff.model.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestra operacoes de usuario sobre o servico Cadastro.
 *
 * Regra de cache: leitura (getById) e cacheada; escritas (update, delete)
 * invalidam a entrada correspondente com @CacheEvict, para o front nunca
 * receber um usuario desatualizado apos uma alteracao. Create nao cacheia
 * (ainda nao ha id estavel para chavear).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserBffService {

    private final CadastroClient cadastroClient;
    private final AppClient appClient;

    public UserResponse create(UserCreateRequest request) {
        log.info("BFF: criando usuario {}", request.email());
        return cadastroClient.create(request);
    }

    @CacheEvict(cacheNames = "usuario", key = "#id")
    public UserResponse update(String id, UserUpdateRequest request) {
        log.info("BFF: atualizando usuario {}", id);
        return cadastroClient.update(id, request);
    }

    @Cacheable(cacheNames = "usuario", key = "#id")
    public UserResponse getById(String id) {
        log.info("BFF: buscando usuario {}", id);
        return cadastroClient.getById(id);
    }

    @CacheEvict(cacheNames = "usuario", key = "#id")
    public void delete(String id) {
        log.info("BFF: deletando usuario {}", id);
        cadastroClient.delete(id);
    }

    public String confirmEmail(String token) {
        return cadastroClient.confirmEmail(token);
    }

    public String resendEmail(String userId) {
        log.info("BFF: reenviando email de confirmacao do usuario {}", userId);
        return cadastroClient.resendEmail(userId);
    }

    public String acceptTerms(String userId) {
        return cadastroClient.acceptTerms(userId);
    }

    public AuthenticationResponse socialLogin(SocialLoginRequest request) {
        log.info("BFF: login social via {}", request.provider());
        return cadastroClient.socialLogin(request);
    }

    public AuthenticationResponse login(LoginRequest request) {
        log.info("BFF: login por senha {}", request.email());
        return cadastroClient.login(request);
    }

    public AuthenticationResponse devLogin(DevLoginRequest request) {
        log.info("BFF: dev login {}", request.email());
        return cadastroClient.devLogin(request);
    }

    public PublicUserResponse findByCode(String userCode) {
        return appClient.findUserByCode(userCode);
    }

    /** Resumo com o usuarioId — o app resolve o codigo do perfil aberto para
     *  entao listar as aventuras visiveis daquele usuario. */
    public UserSummaryResponse findSummaryByCode(String userCode) {
        return appClient.findUserSummaryByCode(userCode);
    }

    public List<PublicUserResponse> autocomplete(String term) {
        return appClient.autocompleteUser(term);
    }
}
