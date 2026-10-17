package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.AdventureParticipant;
import com.app.APP.entity.Media;
import com.app.APP.entity.Path;
import com.app.APP.entity.PointOfInterest;
import com.app.APP.exception.ForbiddenException;
import com.app.APP.mapper.ParticipantMapper;
import com.app.APP.model.dto.response.LeaveAdventureResponse;
import com.app.APP.model.enums.AdventureVisibility;
import com.app.APP.repository.AdventureParticipantRepository;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.EvidenceRepository;
import com.app.APP.repository.MediaRepository;
import com.app.APP.repository.PointOfInterestRepository;
import com.app.APP.repository.PointOfInterestUserStatusRepository;
import com.app.APP.repository.PathRepository;
import com.app.APP.util.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Saida de aventura em grupo. Caminhos sao individuais (cada participante grava
 * o seu); ao sair, o usuario decide se preserva os proprios registros — que sao
 * MOVIDOS para uma aventura pessoal PRIVADA (nao copiados: os pontos GPS vivem no
 * servico de Localizacao chaveados pelo caminho, entao mover a aventura nao os
 * toca) — ou descarta tudo numa cascata manual. O dono nunca sai da propria
 * aventura; quem sai (ou e movido) mantem acesso aos proprios caminhos via
 * AdventureAccessService.canViewPath (dono do caminho sempre ve).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdventureExitService {

    private final AdventureRepository adventureRepository;
    private final AdventureParticipantRepository participantRepository;
    private final PathRepository pathRepository;
    private final MediaRepository mediaRepository;
    private final PointOfInterestRepository pointRepository;
    private final EvidenceRepository evidenceRepository;
    private final PointOfInterestUserStatusRepository statusRepository;

    /** O proprio usuario sai da aventura; keepData decide preservar ou descartar. */
    @Transactional
    public LeaveAdventureResponse leave(String userId, String adventureId, boolean keepData) {
        log.info("User {} leaving adventure {} (keepData={})", userId, adventureId, keepData);
        Adventure adventure = findById(adventureId);

        if (adventure.getUserId().equals(userId)) {
            throw new ForbiddenException("O dono nao sai da propria aventura — exclua ou transfira");
        }
        requireParticipant(adventureId, userId);

        return removeParticipant(adventure, userId, keepData);
    }

    /** O dono remove um participante; os dados do removido sao SEMPRE preservados. */
    @Transactional
    public LeaveAdventureResponse kick(String ownerId, String adventureId, String targetUserId) {
        log.info("Owner {} kicking {} from adventure {}", ownerId, targetUserId, adventureId);
        Adventure adventure = findById(adventureId);
        OwnershipValidator.requireOwner(ownerId, adventure.getUserId(), "Voce nao e o dono desta aventura");

        if (ownerId.equals(targetUserId)) {
            throw new IllegalArgumentException("O dono nao pode se remover da aventura");
        }
        requireParticipant(adventureId, targetUserId);

        return removeParticipant(adventure, targetUserId, true);
    }

    /**
     * Exclui a aventura inteira preservando o que e dos outros: cada participante
     * (exceto o dono) tem os dados movidos para a propria aventura pessoal; os
     * dados do dono caem na cascata; participantes e a aventura sao removidos.
     */
    @Transactional
    public void deleteWithContent(Adventure adventure) {
        String ownerId = adventure.getUserId();
        log.info("Deleting adventure {} with content (owner {})", adventure.getId(), ownerId);

        participantRepository.findByAdventureId(adventure.getId()).stream()
                .map(AdventureParticipant::getUserId)
                .filter(userId -> !userId.equals(ownerId))
                .forEach(userId -> removeParticipant(adventure, userId, true));

        List<Path> ownerPaths = pathRepository.findByAdventureIdAndUserId(adventure.getId(), ownerId);
        List<Media> ownerMedias = mediaRepository.findByAdventureIdAndUserId(adventure.getId(), ownerId);
        discardData(ownerPaths, ownerMedias);

        participantRepository.deleteByAdventureId(adventure.getId());
        adventureRepository.delete(adventure);
        log.info("Adventure {} deleted", adventure.getId());
    }

    private LeaveAdventureResponse removeParticipant(Adventure adventure, String userId, boolean keepData) {
        List<Path> paths = pathRepository.findByAdventureIdAndUserId(adventure.getId(), userId);
        List<Media> medias = mediaRepository.findByAdventureIdAndUserId(adventure.getId(), userId);
        boolean hasData = !paths.isEmpty() || !medias.isEmpty();

        String personalAdventureId = null;
        int moved = 0;
        int deleted = 0;

        if (keepData && hasData) {
            Adventure personal = createPersonalAdventure(adventure, userId);
            paths.forEach(path -> path.setAdventure(personal));
            pathRepository.saveAll(paths);
            medias.forEach(media -> media.setAdventure(personal));
            mediaRepository.saveAll(medias);
            personalAdventureId = personal.getId();
            moved = paths.size();
        } else if (!keepData) {
            discardData(paths, medias);
            deleted = paths.size();
        }

        participantRepository.deleteByAdventureIdAndUserId(adventure.getId(), userId);
        return new LeaveAdventureResponse(personalAdventureId, moved, deleted);
    }

    private Adventure createPersonalAdventure(Adventure origin, String userId) {
        Adventure personal = Adventure.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .destination(origin.getDestination())
                .status(origin.getStatus())
                .visibility(AdventureVisibility.PRIVADA)
                .build();
        adventureRepository.save(personal);
        participantRepository.save(ParticipantMapper.toEntity(personal, userId));
        return personal;
    }

    /**
     * Cascata manual (o modelo nao tem cascade JPA): status pessoal e evidencias
     * pendem dos pontos; pontos pendem dos caminhos; midias sao removidas antes
     * dos caminhos (podem referencia-los). Pontos GPS no loc ficam orfaos (sem
     * endpoint de exclusao por caminho la) — aceito no MVP.
     */
    private void discardData(List<Path> paths, List<Media> medias) {
        if (!paths.isEmpty()) {
            List<String> pathIds = paths.stream().map(Path::getId).toList();
            List<PointOfInterest> points = pointRepository.findByPathIdIn(pathIds);
            List<String> pointIds = points.stream().map(PointOfInterest::getId).toList();
            if (!pointIds.isEmpty()) {
                statusRepository.deleteByPointIdIn(pointIds);
                evidenceRepository.deleteByPointIdIn(pointIds);
            }
            pointRepository.deleteAll(points);
        }
        mediaRepository.deleteAll(medias);
        pathRepository.deleteAll(paths);
    }

    private void requireParticipant(String adventureId, String userId) {
        if (!participantRepository.existsByAdventureIdAndUserId(adventureId, userId)) {
            throw new IllegalArgumentException("Usuario nao participa desta aventura");
        }
    }

    private Adventure findById(String id) {
        return adventureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aventura nao encontrada"));
    }
}
