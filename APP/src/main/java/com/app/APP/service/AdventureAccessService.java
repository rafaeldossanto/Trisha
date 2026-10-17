package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Path;
import com.app.APP.repository.AdventureParticipantRepository;
import com.app.APP.repository.PathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra unica de leitura por visibilidade da aventura, usada por todas as
 * leituras REST (aventura, caminhos, midias e — via BFF — pontos GPS do loc):
 * dono sempre ve; PUBLICA todos; SO_GRUPO so participantes; PRIVADA ninguem.
 * Mesma semantica das queries de descoberta (findVisibleInRegion/ByIds).
 */
@Service
@RequiredArgsConstructor
public class AdventureAccessService {

    private final AdventureParticipantRepository participantRepository;
    private final PathRepository pathRepository;

    public boolean canView(String observerId, Adventure adventure) {
        if (adventure.getUserId().equals(observerId)) {
            return true;
        }
        return switch (adventure.getVisibility()) {
            case PUBLICA -> true;
            case SO_GRUPO -> participantRepository.existsByAdventureIdAndUserId(adventure.getId(), observerId);
            case PRIVADA -> false;
        };
    }

    public void validateView(String observerId, Adventure adventure) {
        if (!canView(observerId, adventure)) {
            throw new IllegalArgumentException("Aventura nao encontrada ou sem acesso");
        }
    }

    @Transactional(readOnly = true)
    public boolean canViewPath(String observerId, String pathId) {
        return pathRepository.findById(pathId)
                .map(path -> path.getUserId().equals(observerId) || canView(observerId, path.getAdventure()))
                .orElse(false);
    }

    public boolean canContribute(String userId, Adventure adventure) {
        return adventure.getUserId().equals(userId)
                || participantRepository.existsByAdventureIdAndUserId(adventure.getId(), userId);
    }

    public void validateContribute(String userId, Adventure adventure) {
        if (!canContribute(userId, adventure)) {
            throw new IllegalArgumentException("Voce nao participa desta aventura");
        }
    }
}
