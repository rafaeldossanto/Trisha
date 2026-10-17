package com.trisha.midia.mapper;

import com.trisha.midia.entity.MediaFile;
import com.trisha.midia.model.dto.response.FileResponse;
import com.trisha.midia.model.enums.FileType;
import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class FileMapper {

    public static MediaFile toEntity(MultipartFile file, FileType type, String storedName,
                                     String bucket, String ownerId, String publicBaseUrl) {
        String id = UUID.randomUUID().toString();
        return MediaFile.builder()
                .id(id)
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .type(type)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .bucket(bucket)
                .url(publicUrl(publicBaseUrl, id))
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * A URL e DERIVADA do id a cada leitura, e nao lida da coluna 'url'. Assim
     * trocar o dominio publico corrige tambem os registros antigos, sem migracao
     * de dados. A coluna continua gravada por compatibilidade.
     */
    public static FileResponse toResponse(MediaFile entity, String publicBaseUrl) {
        return FileResponse.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .type(entity.getType())
                .url(publicUrl(publicBaseUrl, entity.getId()))
                .sizeBytes(entity.getSizeBytes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Endereco publico e PERMANENTE do binario. Substitui a presigned URL que o
     * MinIO assinava com validade maxima de 7 dias e que era persistida (e
     * copiada para Media.url e Region.coverUrl no servico APP) — passado o prazo,
     * toda foto do app quebrava. O objeto no bucket so e removido quando o dono
     * chama DELETE /arquivo/{id}.
     */
    public static String publicUrl(String publicBaseUrl, String id) {
        return publicBaseUrl + "/arquivo/" + id + "/conteudo";
    }
}
