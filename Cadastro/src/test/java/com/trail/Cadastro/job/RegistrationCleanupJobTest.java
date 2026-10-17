package com.trail.Cadastro.job;

import com.trail.Cadastro.repository.UserRepository;
import com.trail.Cadastro.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationCleanupJob")
class RegistrationCleanupJobTest {

    private static final int EXPIRACAO_MINUTOS = 10;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private RegistrationCleanupJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "expirationMinutes", EXPIRACAO_MINUTOS);
    }

    @Test
    @DisplayName("desativa cada cadastro pendente que estourou o prazo")
    void deveExpirarPendentes() {
        when(userRepository.findExpiredPendingIds(any())).thenReturn(List.of("id-1", "id-2"));

        job.expirePendingRegistrations();

        verify(userService).delete("id-1");
        verify(userService).delete("id-2");
    }

    @Test
    @DisplayName("nao toca em ninguem quando nao ha pendente vencido")
    void naoDeveFazerNadaSemPendentes() {
        when(userRepository.findExpiredPendingIds(any())).thenReturn(List.of());

        job.expirePendingRegistrations();

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("consulta usando o prazo configurado como corte")
    void deveUsarOPrazoConfigurado() {
        when(userRepository.findExpiredPendingIds(any())).thenReturn(List.of());
        LocalDateTime antes = LocalDateTime.now().minusMinutes(EXPIRACAO_MINUTOS);

        job.expirePendingRegistrations();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).findExpiredPendingIds(captor.capture());
        assertThat(captor.getValue())
                .isBetween(antes.minusSeconds(5), antes.plusSeconds(5));
    }

    /**
     * Uma linha problematica nao pode abortar a varredura: o resto do lote
     * precisa ser processado no mesmo ciclo.
     */
    @Test
    @DisplayName("segue para os proximos quando um cadastro falha ao ser desativado")
    void deveSeguirAposFalhaIsolada() {
        when(userRepository.findExpiredPendingIds(any())).thenReturn(List.of("id-1", "id-2", "id-3"));
        doThrow(new IllegalArgumentException("Usuario nao encontrado"))
                .when(userService).delete("id-2");

        job.expirePendingRegistrations();

        verify(userService).delete("id-1");
        verify(userService).delete("id-2");
        verify(userService).delete("id-3");
    }

    @Test
    @DisplayName("nao desativa quando o repositorio nao devolve nada, mesmo apos varios ciclos")
    void deveSerEstavelEntreCiclos() {
        when(userRepository.findExpiredPendingIds(any())).thenReturn(List.of());

        job.expirePendingRegistrations();
        job.expirePendingRegistrations();

        verify(userService, never()).delete(any());
    }
}
