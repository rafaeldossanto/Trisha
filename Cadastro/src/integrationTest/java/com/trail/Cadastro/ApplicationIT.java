package com.trail.Cadastro;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de integracao do Cadastro: sobe o contexto Spring completo contra um
 * PostgreSQL real (Testcontainers), validando a camada JPA/web.
 *
 * O cadastro nao depende mais de broker externo — confirmacao de email, aceite
 * de termos e prazo de expiracao sao resolvidos no proprio banco —, entao o
 * contexto sobe apenas com o Postgres do container.
 *
 * A varredura de expiracao fica desligada aqui (intervalo muito alto): ela roda
 * em thread propria e nao deve concorrer com os testes.
 */
@Tag("integracao")
@SpringBootTest
// Profile de desenvolvimento explicito: sem ele o JwtKeyConfig aborta o boot por
// falta de JWT_RSA_PRIVATE_KEY_PATH (fail closed), que e exatamente o
// comportamento desejado em qualquer ambiente que nao seja dev/test.
@ActiveProfiles("test")
@Testcontainers
class ApplicationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("trilha_cadastro");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.cadastro.expiracao-minutos", () -> "10");
        registry.add("app.cadastro.intervalo-limpeza-ms", () -> "3600000");
    }

    @Test
    void contextLoads() {
    }
}
