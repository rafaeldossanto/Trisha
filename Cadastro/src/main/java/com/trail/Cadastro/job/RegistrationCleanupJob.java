package com.trail.Cadastro.job;

import com.trail.Cadastro.repository.UserRepository;
import com.trail.Cadastro.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cobra o prazo de confirmacao do cadastro: quem fica PENDENTE alem do limite e
 * desativado, liberando o email para uma nova tentativa.
 *
 * Nota: UserService.delete desativa (status INATIVO), nao apaga a linha — o
 * mesmo comportamento de antes.
 *
 * Escala: com mais de uma instancia do Cadastro todas rodariam a varredura. Hoje
 * o servico e instancia unica; ao escalar horizontalmente, avaliar ShedLock. Um
 * ciclo concorrente nao corrompe dado (o delete e idempotente), so duplica
 * trabalho.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationCleanupJob {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.cadastro.expiracao-minutos}")
    private int expirationMinutes;

    @Scheduled(fixedDelayString = "${app.cadastro.intervalo-limpeza-ms}")
    public void expirePendingRegistrations() {
        LocalDateTime limit = LocalDateTime.now().minusMinutes(expirationMinutes);
        List<String> expired = userRepository.findExpiredPendingIds(limit);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Expirando {} cadastro(s) pendente(s) sem confirmacao ha mais de {} min",
                expired.size(), expirationMinutes);

        expired.forEach(this::expire);
    }

    /**
     * Uma falha isolada nao pode interromper a varredura: o proximo ciclo tenta
     * de novo o que ficou para tras.
     */
    private void expire(String userId) {
        try {
            userService.delete(userId);
        } catch (Exception e) {
            log.error("Falha ao expirar o cadastro {}: {}", userId, e.getMessage());
        }
    }
}
