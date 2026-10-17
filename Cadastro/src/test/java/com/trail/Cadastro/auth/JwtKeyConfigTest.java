package com.trail.Cadastro.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtKeyConfig")
class JwtKeyConfigTest {

    private static final String PROD_KEY_PATH = "app.jwt.rsa-private-key-path";

    @Test
    @DisplayName("deve abortar quando nenhum profile esta ativo e a chave nao foi configurada")
    void deveAbortarSemProfileESemChave() {
        // Cenario do deploy mal configurado: SPRING_PROFILES_ACTIVE esquecido.
        // Antes o servico subia assinando tokens com a chave publica do
        // repositorio, e qualquer pessoa podia forjar o JWT de qualquer usuario.
        JwtKeyConfig config = configWith(new MockEnvironment(), "");

        assertThatThrownBy(config::rsaKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_RSA_PRIVATE_KEY_PATH nao definida");
    }

    @Test
    @DisplayName("deve abortar em profile que nao e de desenvolvimento (ex.: staging)")
    void deveAbortarEmProfileDesconhecido() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");

        assertThatThrownBy(configWith(env, "")::rsaKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("profiles de desenvolvimento");
    }

    @Test
    @DisplayName("deve abortar em prod sem chave configurada")
    void deveAbortarEmProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(configWith(env, "")::rsaKey)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("deve aceitar a chave bundled com profile dev ativo")
    void deveUsarChaveBundledEmDev() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        assertThatCode(configWith(env, "")::rsaKey).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deve aceitar a chave bundled com profile test ativo")
    void deveUsarChaveBundledEmTest() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        assertThatCode(configWith(env, "")::rsaKey).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("kid deve ser estavel entre carregamentos da mesma chave")
    void kidDeveSerEstavel() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        String primeiro = configWith(env, "").rsaKey().getKeyID();
        String segundo = configWith(env, "").rsaKey().getKeyID();

        assertThat(primeiro).isEqualTo(segundo);
    }

    private JwtKeyConfig configWith(MockEnvironment environment, String keyPath) {
        JwtProperties properties = new JwtProperties();
        properties.setRsaPrivateKeyPath(keyPath);
        environment.setProperty(PROD_KEY_PATH, keyPath);
        return new JwtKeyConfig(properties, environment);
    }
}
