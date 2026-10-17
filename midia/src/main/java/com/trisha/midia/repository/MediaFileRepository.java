package com.trisha.midia.repository;

import com.trisha.midia.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MediaFileRepository extends JpaRepository<MediaFile, String> {

    /** Uploads recentes do usuario — trava de taxa contra upload automatizado. */
    long countByOwnerIdAndCreatedAtAfter(String ownerId, LocalDateTime since);

    /**
     * Bytes ja armazenados pelo usuario. O disco da maquina e o recurso mais
     * caro de escalar, e o upload nao passa pelo rate limit do BFF — sem esta
     * conta, uma conta so poderia encher o volume do MinIO.
     */
    @Query("select coalesce(sum(f.sizeBytes), 0) from MediaFile f where f.ownerId = :ownerId")
    long sumSizeBytesByOwnerId(@Param("ownerId") String ownerId);
}
