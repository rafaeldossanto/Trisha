package com.app.APP.repository;

import com.app.APP.entity.Media;
import com.app.APP.model.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, String> {
    Page<Media> findByAdventureId(String adventureId, Pageable pageable);
    Page<Media> findByPathId(String pathId, Pageable pageable);
    List<Media> findByAdventureIdAndType(String adventureId, MediaType type);

    /** Midias de um usuario dentro de uma aventura (incl. avulsas) — usado ao sair/remover. */
    List<Media> findByAdventureIdAndUserId(String adventureId, String userId);
}
