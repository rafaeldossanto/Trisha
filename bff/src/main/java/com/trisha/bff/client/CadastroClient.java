package com.trisha.bff.client;

import com.trisha.bff.exception.ClientFallbacks;
import com.trisha.bff.exception.ServiceUnavailableException;
import com.trisha.bff.model.dto.request.DevLoginRequest;
import com.trisha.bff.model.dto.request.LoginRequest;
import com.trisha.bff.model.dto.request.SocialLoginRequest;
import com.trisha.bff.model.dto.request.UserCreateRequest;
import com.trisha.bff.model.dto.request.UserUpdateRequest;
import com.trisha.bff.model.dto.response.AuthenticationResponse;
import com.trisha.bff.model.dto.response.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class CadastroClient {

    private final RestClient cadastroRestClient;

    public CadastroClient(@Qualifier("cadastroRestClient") RestClient cadastroRestClient) {
        this.cadastroRestClient = cadastroRestClient;
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackCreate")
    @Retry(name = "cadastro")
    public UserResponse create(UserCreateRequest request) {
        log.debug("CADASTRO: criando usuario {}", request.email());
        return cadastroRestClient.post()
                .uri("/usuario")
                .body(request)
                .retrieve()
                .body(UserResponse.class);
    }

    public UserResponse fallbackCreate(UserCreateRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao criar usuario - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackUpdate")
    @Retry(name = "cadastro")
    public UserResponse update(String id, UserUpdateRequest request) {
        log.debug("CADASTRO: atualizando usuario {}", id);
        return cadastroRestClient.put()
                .uri("/usuario/{id}", id)
                .body(request)
                .retrieve()
                .body(UserResponse.class);
    }

    public UserResponse fallbackUpdate(String id, UserUpdateRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao atualizar usuario {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackGetById")
    @Retry(name = "cadastro")
    public UserResponse getById(String id) {
        log.debug("CADASTRO: buscando usuario {}", id);
        return cadastroRestClient.get()
                .uri("/usuario/{id}", id)
                .retrieve()
                .body(UserResponse.class);
    }

    public UserResponse fallbackGetById(String id, Throwable t) {
        log.error("Circuit breaker: falha ao buscar usuario {} - {}", id, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    public void delete(String id) {
        log.debug("CADASTRO: deletando usuario {}", id);
        cadastroRestClient.delete()
                .uri("/usuario/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackConfirmEmail")
    @Retry(name = "cadastro")
    public String confirmEmail(String token) {
        log.debug("CADASTRO: confirmando email com token");
        return cadastroRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/auth/confirmar-email").queryParam("token", token).build())
                .retrieve()
                .body(String.class);
    }

    public String fallbackConfirmEmail(String token, Throwable t) {
        log.error("Circuit breaker: falha ao confirmar email - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackResendEmail")
    @Retry(name = "cadastro")
    public String resendEmail(String userId) {
        log.debug("CADASTRO: reenviando email de confirmacao do usuario {}", userId);
        return cadastroRestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/auth/reenviar-email").queryParam("usuarioId", userId).build())
                .retrieve()
                .body(String.class);
    }

    public String fallbackResendEmail(String userId, Throwable t) {
        log.error("Circuit breaker: falha ao reenviar email do usuario {} - {}", userId, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackAcceptTerms")
    @Retry(name = "cadastro")
    public String acceptTerms(String userId) {
        log.debug("CADASTRO: aceitando termos do usuario {}", userId);
        return cadastroRestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/auth/aceitar-termos").queryParam("usuarioId", userId).build())
                .retrieve()
                .body(String.class);
    }

    public String fallbackAcceptTerms(String userId, Throwable t) {
        log.error("Circuit breaker: falha ao aceitar termos do usuario {} - {}", userId, t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackSocialLogin")
    @Retry(name = "cadastro")
    public AuthenticationResponse socialLogin(SocialLoginRequest request) {
        log.debug("CADASTRO: login social via {}", request.provider());
        return cadastroRestClient.post()
                .uri("/auth/social")
                .body(request)
                .retrieve()
                .body(AuthenticationResponse.class);
    }

    public AuthenticationResponse fallbackSocialLogin(SocialLoginRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao realizar login social - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackLogin")
    @Retry(name = "cadastro")
    public AuthenticationResponse login(LoginRequest request) {
        log.debug("CADASTRO: login por senha {}", request.email());
        return cadastroRestClient.post()
                .uri("/auth/login")
                .body(request)
                .retrieve()
                .body(AuthenticationResponse.class);
    }

    public AuthenticationResponse fallbackLogin(LoginRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao realizar login - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }

    @CircuitBreaker(name = "cadastro", fallbackMethod = "fallbackDevLogin")
    @Retry(name = "cadastro")
    public AuthenticationResponse devLogin(DevLoginRequest request) {
        log.debug("CADASTRO: dev login {}", request.email());
        return cadastroRestClient.post()
                .uri("/auth/dev-login")
                .body(request)
                .retrieve()
                .body(AuthenticationResponse.class);
    }

    public AuthenticationResponse fallbackDevLogin(DevLoginRequest request, Throwable t) {
        log.error("Circuit breaker: falha ao realizar dev login - {}", t.getMessage());
        throw ClientFallbacks.unavailable(t, "Servico temporariamente indisponivel. Tente novamente em breve.");
    }
}
