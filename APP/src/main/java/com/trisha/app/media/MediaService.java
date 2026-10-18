package com.trisha.app.media;

import com.trisha.app.adventure.Adventure;
import com.trisha.app.adventure.AdventureAccessService;
import com.trisha.app.adventure.AdventureRepository;
import com.trisha.app.media.dto.MediaRequest;
import com.trisha.app.media.dto.MediaResponse;
import com.trisha.app.path.Path;
import com.trisha.app.path.PathRepository;
import com.trisha.app.shared.util.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final AdventureRepository adventureRepository;
    private final PathRepository pathRepository;
    private final AdventureAccessService accessService;

    @Transactional
    public MediaResponse save(String userId, MediaRequest request) {
        log.info("Saving media type: {} in adventure: {}", request.type(), request.adventureId());

        Adventure adventure = adventureRepository.findById(request.adventureId())
                .orElseThrow(() -> new IllegalArgumentException("Aventura nao encontrada"));
        accessService.validateContribute(userId, adventure);

        Path path = null;
        if (nonNull(request.pathId())) {
            path = pathRepository.findById(request.pathId())
                    .orElseThrow(() -> new IllegalArgumentException("Caminho nao encontrado"));
            if (!path.getAdventure().getId().equals(adventure.getId())) {
                throw new IllegalArgumentException("O caminho nao pertence a essa aventura");
            }
        }

        Media media = mediaRepository.save(MediaMapper.toEntity(request, adventure, path, userId));

        return MediaMapper.toResponse(media);
    }

    @Transactional(readOnly = true)
    public Page<MediaResponse> getByAdventure(String observerId, String adventureId, Pageable pageable) {
        Adventure adventure = adventureRepository.findById(adventureId)
                .orElseThrow(() -> new IllegalArgumentException("Aventura nao encontrada"));
        accessService.validateView(observerId, adventure);
        return mediaRepository.findByAdventureId(adventureId, pageable)
                .map(MediaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MediaResponse> getByPath(String observerId, String pathId, Pageable pageable) {
        Path path = pathRepository.findById(pathId)
                .orElseThrow(() -> new IllegalArgumentException("Caminho nao encontrado"));
        accessService.validateView(observerId, path.getAdventure());
        return mediaRepository.findByPathId(pathId, pageable)
                .map(MediaMapper::toResponse);
    }

    @Transactional
    public void delete(String userId, String id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Midia nao encontrada"));

        OwnershipValidator.requireOwner(userId, media.getUserId(), "Voce nao e o dono desta midia");

        mediaRepository.delete(media);
        log.info("Media {} deleted", id);
    }

    /** Move as midias do usuario na aventura (incl. avulsas) para {@code target}. */
    @Transactional
    public void reassign(String adventureId, String userId, Adventure target) {
        List<Media> medias = mediaRepository.findByAdventureIdAndUserId(adventureId, userId);
        medias.forEach(media -> media.setAdventure(target));
        mediaRepository.saveAll(medias);
    }

    /** Apaga as midias do usuario na aventura (incl. avulsas). */
    @Transactional
    public void deleteByAdventureAndUser(String adventureId, String userId) {
        mediaRepository.deleteAll(mediaRepository.findByAdventureIdAndUserId(adventureId, userId));
    }
}
