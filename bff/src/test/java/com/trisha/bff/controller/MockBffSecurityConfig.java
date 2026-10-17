package com.trisha.bff.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import java.util.List;

/**
 * Configuracao de seguranca para testes de controller do BFF (@WebMvcTest).
 *
 * Cria uma SecurityFilterChain minima com SecurityContextHolderFilter e aplica
 * SecurityMockMvcConfigurers.springSecurity(filterChainProxy) ao MockMvc via
 * MockMvcBuilderCustomizer, permitindo que .with(jwt()) popule corretamente o
 * SecurityContextHolder para os endpoints autenticados.
 */
@TestConfiguration
class MockBffSecurityConfig {

    @Bean
    SecurityFilterChain bffTestSecurityFilterChain() {
        var repo = new RequestAttributeSecurityContextRepository();
        var holderFilter = new SecurityContextHolderFilter(repo);
        return new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE, List.of(holderFilter));
    }

    @Bean
    MockMvcBuilderCustomizer bffSecurityMockMvcCustomizer(SecurityFilterChain bffTestSecurityFilterChain) {
        var proxy = new FilterChainProxy(List.of(bffTestSecurityFilterChain));
        return builder -> builder.apply(SecurityMockMvcConfigurers.springSecurity(proxy));
    }
}
