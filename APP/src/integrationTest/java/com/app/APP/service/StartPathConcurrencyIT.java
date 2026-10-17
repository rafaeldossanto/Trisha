package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Path;
import com.app.APP.model.dto.request.PathRequest;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.PathRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressao da corrida no numero do caminho. O numero vem de
 * {@code countByAdventureId()+1}; caminhos sao individuais e varios participantes
 * de uma aventura em grupo podem inicia-los ao mesmo tempo — sem serializacao,
 * gravacoes concorrentes na MESMA aventura geram numero duplicado. Este teste
 * sobe um Postgres real e dispara N inicios simultaneos na mesma aventura: com o
 * lock pessimista na aventura, os numeros saem exatamente 1..N, sem duplicata.
 */
@Tag("integracao")
@SpringBootTest
@Testcontainers
@DisplayName("PathService.start (concorrencia)")
class StartPathConcurrencyIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("trilha_app");

    @DynamicPropertySource
    static void jpaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    private static final String OWNER_ID = "dono-aventura";
    private static final int CONCURRENT_STARTS = 50;

    @Autowired
    private PathService pathService;
    @Autowired
    private AdventureRepository adventureRepository;
    @Autowired
    private PathRepository pathRepository;

    @AfterEach
    void cleanUp() {
        pathRepository.deleteAll();
        adventureRepository.deleteAll();
    }

    @Test
    @DisplayName("inicios concorrentes na mesma aventura geram numeros unicos e sequenciais")
    void concurrentStartYieldsUniqueSequentialNumbers() throws InterruptedException {
        Adventure adventure = adventureRepository.save(Adventure.builder()
                .id(UUID.randomUUID().toString())
                .userId(OWNER_ID)
                .destination("Pico da Bandeira")
                .build());

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_STARTS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(CONCURRENT_STARTS);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < CONCURRENT_STARTS; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    pathService.start(OWNER_ID, new PathRequest(adventure.getId(), null));
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGate.countDown(); // libera todas as threads de uma vez para maximizar a contencao
        boolean completed = finished.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(completed).as("todos os inicios concluiram no tempo").isTrue();
        assertThat(failures.get()).as("nenhum inicio falhou").isZero();

        List<Integer> numbers = pathRepository.findByAdventureIdAndUserId(adventure.getId(), OWNER_ID)
                .stream().map(Path::getNumber).sorted().toList();

        List<Integer> expected = IntStream.rangeClosed(1, CONCURRENT_STARTS).boxed().toList();
        assertThat(numbers)
                .as("numeros 1..N sem duplicata nem buraco")
                .containsExactlyElementsOf(expected);
    }
}
