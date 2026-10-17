package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.mapper.AdventureMapper;
import com.app.APP.model.dto.response.AdventureResponse;
import com.app.APP.repository.AdventureParticipantRepository;
import com.app.APP.repository.PathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriquece a AdventureResponse com metricas derivadas (nao armazenadas):
 * numero de participantes e duracao em horas — janela de parede da aventura,
 * do primeiro caminho iniciado ao ultimo finalizado. A duracao e nula enquanto
 * nenhum caminho finalizou. Nas listas as metricas vem em lote (2 queries por
 * pagina) para evitar N+1.
 */
@Service
@RequiredArgsConstructor
public class AdventureMetricsAssembler {

    private final AdventureParticipantRepository participantRepository;
    private final PathRepository pathRepository;

    @Transactional(readOnly = true)
    public AdventureResponse build(Adventure adventure) {
        String id = adventure.getId();
        long participants = participantRepository.countByAdventureId(id);
        Double hours = durationsByAdventure(List.of(id)).get(id);
        return AdventureMapper.toResponse(adventure, participants, hours);
    }

    @Transactional(readOnly = true)
    public Page<AdventureResponse> buildPage(Page<Adventure> page) {
        List<String> ids = page.getContent().stream().map(Adventure::getId).toList();
        Map<String, Long> counts = participantCounts(ids);
        Map<String, Double> durations = durationsByAdventure(ids);
        return page.map(adventure -> AdventureMapper.toResponse(
                adventure,
                counts.getOrDefault(adventure.getId(), 0L),
                durations.get(adventure.getId())));
    }

    private Map<String, Long> participantCounts(List<String> ids) {
        Map<String, Long> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        for (Object[] row : participantRepository.countByAdventureIds(ids)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Map<String, Double> durationsByAdventure(List<String> ids) {
        Map<String, Double> durations = new HashMap<>();
        if (ids.isEmpty()) {
            return durations;
        }
        for (Object[] row : pathRepository.findTimespansByAdventureIds(ids)) {
            Double hours = durationHours((LocalDateTime) row[1], (LocalDateTime) row[2]);
            if (hours != null) {
                durations.put((String) row[0], hours);
            }
        }
        return durations;
    }

    private Double durationHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return null;
        }
        long minutes = Duration.between(start, end).toMinutes();
        return Math.round(minutes / 60.0 * 10) / 10.0;
    }
}
