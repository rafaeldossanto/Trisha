package com.app.APP.controller;

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
 * Configuracao de seguranca para testes de controller (@WebMvcTest).
 *
 * Estrategia:
 *  1) Registra uma SecurityFilterChain minima (impede que OAuth2ResourceServerAutoConfiguration
 *     tente criar jwtSecurityFilterChain que requere HttpSecurity).
 *  2) Cria um FilterChainProxy wrapping essa cadeia e aplica
 *     SecurityMockMvcConfigurers.springSecurity(filterChainProxy) ao MockMvc.
 *     O WebTestUtils.findFilter() navega pelo FilterChainProxy para encontrar o
 *     SecurityContextHolderFilter e faz ReflectionTestUtils.setField() no repositorio,
 *     permitindo que .with(jwt()) popule o SecurityContextHolder corretamente.
 */
@TestConfiguration
class MockSecurityConfig {

    @Bean
    SecurityFilterChain testSecurityFilterChain() {
        var repo = new RequestAttributeSecurityContextRepository();
        var holderFilter = new SecurityContextHolderFilter(repo);
        return new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE, List.of(holderFilter));
    }

    @Bean
    MockMvcBuilderCustomizer securityMockMvcCustomizer(SecurityFilterChain testSecurityFilterChain) {
        var proxy = new FilterChainProxy(List.of(testSecurityFilterChain));
        return builder -> builder.apply(SecurityMockMvcConfigurers.springSecurity(proxy));
    }
}
