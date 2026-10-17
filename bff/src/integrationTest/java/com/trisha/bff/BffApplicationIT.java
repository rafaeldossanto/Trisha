package com.trisha.bff;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Teste de integracao: sobe o contexto Spring completo contra um Redis real
 * provisionado pelo Testcontainers. Garante que a config de cache (RedisConfig)
 * e os beans de RestClient inicializam corretamente. O container e criado e
 * destruido pelo teste — sem dependencia de docker compose manual.
 *
 * As URLs dos downstreams ficam com o default de localhost; o contexto sobe
 * sem precisar que os servicos estejam no ar (os RestClients sao lazy quanto a
 * conexao — so conectam quando chamados).
 */
@Tag("integracao")
@SpringBootTest
@Testcontainers
class BffApplicationIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void contextLoads() {
    }
}
