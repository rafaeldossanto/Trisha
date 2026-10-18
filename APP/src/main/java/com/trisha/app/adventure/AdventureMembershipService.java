package com.trisha.app.adventure;

import com.trisha.app.adventure.dto.LeaveAdventureResponse;
import com.trisha.app.media.MediaRepository;
import com.trisha.app.media.MediaService;
import com.trisha.app.path.Path;
import com.trisha.app.path.PathRepository;
import com.trisha.app.path.PathService;
import com.trisha.app.point.PointOfInterestService;
import com.trisha.app.shared.exception.ForbiddenException;
import com.trisha.app.shared.util.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quem participa de uma aventura em grupo: entrar, sair, ser removido e a
 * exclusao da aventura inteira. Sao as unicas operacoes que atravessam aventura,
 * caminho, midia e ponto; este servico coordena e cada area apaga ou move os
 * proprios dados pelo seu service.
 *
 * Caminhos sao individuais (cada participante grava o seu); ao sair, o usuario
 * decide se preserva os proprios registros — que sao MOVIDOS para uma aventura
 * pessoal PRIVADA (nao copiados: os pontos GPS vivem no servico de Localizacao
 * chaveados pelo caminho, entao mover a aventura nao os toca) — ou descarta tudo
 * numa cascata manual. O dono nunca sai da propria aventura; quem sai (ou e
 * movido) mantem acesso aos proprios caminhos via PathService.canView (dono do
 * caminho sempre ve).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdventureMembershipService {

    private final AdventureRepository adventureRepository;
    private final AdventureParticipantRepository participantRepository;
    private final PathRepository pathRepository;
    private final MediaRepository mediaRepository;
    private final AdventureService adventureService;
    private final PathService pathService;
    private final MediaService mediaService;
    private final PointOfInterestService pointService;

    @Transactional
    public void addParticipant(String ownerId, String adventureId, String userId) {
        Adventure adventure = findOwner(ownerId, adventureId);

        if (participantRepository.existsByAdventureIdAndUserId(adventureId, userId)) {
            throw new IllegalArgumentException("Usuario ja participa dessa aventura");
        }

        participantRepository.save(ParticipantMapper.toEntity(adventure, userId));
        log.info("User {} added to adventure {}", userId, adventureId);
    }

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
    public void delete(String userId, String adventureId) {
        Adventure adventure = findOwner(userId, adventureId);
        String ownerId = adventure.getUserId();
        log.info("Deleting adventure {} with content (owner {})", adventureId, ownerId);

        participantRepository.findByAdventureId(adventureId).stream()
                .map(AdventureParticipant::getUserId)
                .filter(participantId -> !participantId.equals(ownerId))
                .forEach(participantId -> removeParticipant(adventure, participantId, true));

        discardData(adventureId, ownerId, pathIds(adventureId, ownerId));

        participantRepository.deleteByAdventureId(adventureId);
        adventureRepository.delete(adventure);
        log.info("Adventure {} deleted", adventureId);
    }

    private LeaveAdventureResponse removeParticipant(Adventure adventure, String userId, boolean keepData) {
        String adventureId = adventure.getId();
        List<String> pathIds = pathIds(adventureId, userId);
        boolean hasData = !pathIds.isEmpty() || !mediaRepository.findByAdventureIdAndUserId(adventureId, userId).isEmpty();

        String personalAdventureId = null;
        int moved = 0;
        int deleted = 0;

        if (keepData && hasData) {
            Adventure personal = adventureService.createPersonalCopy(adventure, userId);
            moved = pathService.reassign(adventureId, userId, personal);
            mediaService.reassign(adventureId, userId, personal);
            personalAdventureId = personal.getId();
        } else if (!keepData) {
            deleted = discardData(adventureId, userId, pathIds);
        }

        participantRepository.deleteByAdventureIdAndUserId(adventureId, userId);
        return new LeaveAdventureResponse(personalAdventureId, moved, deleted);
    }

    /**
     * Cascata manual (o modelo nao tem cascade JPA), na ordem das FKs: os pontos
     * (com status pessoal e evidencias) pendem dos caminhos; midias sao removidas
     * antes dos caminhos (podem referencia-los). Pontos GPS no loc ficam orfaos
     * (sem endpoint de exclusao por caminho la) — aceito no MVP. Devolve quantos
     * caminhos foram excluidos.
     */
    private int discardData(String adventureId, String userId, List<String> pathIds) {
        pointService.deleteByPathIds(pathIds);
        mediaService.deleteByAdventureAndUser(adventureId, userId);
        return pathService.deleteByAdventureAndUser(adventureId, userId);
    }

    private List<String> pathIds(String adventureId, String userId) {
        return pathRepository.findByAdventureIdAndUserId(adventureId, userId).stream()
                .map(Path::getId)
                .toList();
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

    private Adventure findOwner(String userId, String id) {
        Adventure adventure = findById(id);
        OwnershipValidator.requireOwner(userId, adventure.getUserId(), "Voce nao e o dono desta aventura");
        return adventure;
    }
}
