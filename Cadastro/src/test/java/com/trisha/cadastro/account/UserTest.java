package com.trisha.cadastro.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User (transicoes de status)")
class UserTest {

    private static final LocalDateTime ONTEM = LocalDateTime.now().minusDays(1);

    private User userWith(RegistrationStatus status) {
        return User.builder()
                .id("id-123")
                .name("Rafael")
                .email("rafael@email.com")
                .password("hash-antigo")
                .userCode("rafael#1")
                .status(status)
                .createdAt(ONTEM)
                .updatedAt(ONTEM)
                .build();
    }

    @Test
    @DisplayName("isActive so e verdadeiro para ATIVO")
    void isActive() {
        assertThat(userWith(RegistrationStatus.ATIVO).isActive()).isTrue();
        assertThat(userWith(RegistrationStatus.PENDENTE).isActive()).isFalse();
        assertThat(userWith(RegistrationStatus.INATIVO).isActive()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = RegistrationStatus.class, names = {"PENDENTE", "INATIVO"})
    @DisplayName("activate deixa ATIVO e grava o instante informado no updatedAt")
    void activate(RegistrationStatus from) {
        User user = userWith(from);
        LocalDateTime now = LocalDateTime.now();

        user.activate(now);

        assertThat(user.getStatus()).isEqualTo(RegistrationStatus.ATIVO);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("deactivate deixa INATIVO sem mexer no updatedAt")
    void deactivate() {
        User user = userWith(RegistrationStatus.ATIVO);

        user.deactivate();

        assertThat(user.getStatus()).isEqualTo(RegistrationStatus.INATIVO);
        assertThat(user.getUpdatedAt()).isEqualTo(ONTEM);
    }

    @Test
    @DisplayName("reopen volta para PENDENTE com nome e senha novos, preservando id, codigo e criacao")
    void reopen() {
        User user = userWith(RegistrationStatus.INATIVO);

        user.reopen("Rafael Novo", "hash-novo");

        assertThat(user.getStatus()).isEqualTo(RegistrationStatus.PENDENTE);
        assertThat(user.getName()).isEqualTo("Rafael Novo");
        assertThat(user.getPassword()).isEqualTo("hash-novo");
        // O prazo de confirmacao recomeca: o RegistrationCleanupJob corta pelo updatedAt.
        assertThat(user.getUpdatedAt()).isAfter(ONTEM);
        assertThat(user.getId()).isEqualTo("id-123");
        assertThat(user.getUserCode()).isEqualTo("rafael#1");
        assertThat(user.getCreatedAt()).isEqualTo(ONTEM);
    }
}
