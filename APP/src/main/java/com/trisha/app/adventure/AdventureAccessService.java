package com.trisha.app.adventure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    /**
     * A versao JPQL desta regra, usada nas queries de listagem e descoberta, e
     * {@link AdventureVisibilityJpql#VISIBLE_TO_OBSERVER}.
     */
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
