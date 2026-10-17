package com.trisha.midia.service;

import com.trisha.midia.entity.MediaFile;
import com.trisha.midia.exception.ForbiddenException;
import com.trisha.midia.exception.QuotaExceededException;
import com.trisha.midia.mapper.FileMapper;
import com.trisha.midia.model.dto.response.FileContent;
import com.trisha.midia.model.dto.response.FileResponse;
import com.trisha.midia.model.enums.FileType;
import com.trisha.midia.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    /**
     * Whitelist de formatos, em vez de aceitar qualquer {@code image/*}. O motivo
     * e o {@code image/svg+xml}: SVG pode carregar JavaScript e, servido pelo
     * endpoint publico de conteudo, executaria script no dominio da API — XSS
     * armazenado, com o arquivo hospedado por nos. Whitelist tambem barra
     * formatos exoticos que nenhum cliente do app produz.
     */
    private static final Set<String> ALLOWED_PHOTO_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private static final Set<String> ALLOWED_VIDEO_TYPES =
            Set.of("video/mp4", "video/quicktime", "video/webm");

    private final MinioService minioService;
    private final MediaFileRepository repository;

    /** Base publica do proprio servico de Midia, usada para montar a URL do binario. */
    @Value("${midia.public-url}")
    private String publicBaseUrl;

    @Value("${midia.upload.max-por-hora}")
    private int maxUploadsPerHour;

    @Value("${midia.upload.cota-mb}")
    private long storageQuotaMb;

    public FileResponse upload(MultipartFile file, FileType type, String ownerId) {
        log.info("Iniciando upload: {} ({})", file.getOriginalFilename(), type);

        validateFile(file, type);
        validateQuota(ownerId, file.getSize());

        String storedName = extractStoredName(file.getOriginalFilename());
        minioService.upload(storedName, file);

        MediaFile entity = FileMapper.toEntity(file, type, storedName, minioService.getBucket(),
                ownerId, publicBaseUrl);
        repository.save(entity);

        log.info("Arquivo salvo com id: {}", entity.getId());
        return FileMapper.toResponse(entity, publicBaseUrl);
    }

    public FileResponse getById(String id) {
        return FileMapper.toResponse(findById(id), publicBaseUrl);
    }

    /**
     * Binario do arquivo, servido pelo proprio servico. E o que sustenta a URL
     * permanente: o bucket fica privado e nada expira.
     */
    public FileContent download(String id) {
        MediaFile file = findById(id);
        return new FileContent(file.getContentType(), file.getSizeBytes(),
                minioService.download(file.getStoredName()));
    }

    public void delete(String id, String userId) {
        MediaFile file = findById(id);

        if (!userId.equals(file.getOwnerId())) {
            throw new ForbiddenException("Voce nao e o dono deste arquivo");
        }

        minioService.delete(file.getStoredName());
        repository.delete(file);
        log.info("Arquivo {} deletado", id);
    }

    private MediaFile findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Arquivo nao encontrado"));
    }

    private void validateFile(MultipartFile file, FileType type) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (isNull(contentType)) {
            throw new IllegalArgumentException("Tipo do arquivo nao identificado");
        }

        Set<String> allowed = FileType.FOTO.equals(type) ? ALLOWED_PHOTO_TYPES : ALLOWED_VIDEO_TYPES;
        if (!allowed.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo %s nao aceita '%s'. Formatos aceitos: %s"
                            .formatted(type, contentType, String.join(", ", allowed)));
        }
    }

    /**
     * Trava de abuso do upload. O binario vai direto ao servico de Midia, sem
     * passar pelo BFF — logo, sem o rate limit por IP da borda. Sem estas duas
     * contas, uma conta autenticada poderia encher o volume do MinIO.
     */
    private void validateQuota(String ownerId, long incomingBytes) {
        long recentUploads = repository.countByOwnerIdAndCreatedAtAfter(
                ownerId, LocalDateTime.now().minusHours(1));
        if (recentUploads >= maxUploadsPerHour) {
            throw new QuotaExceededException(
                    "Limite de %d envios por hora atingido. Tente novamente mais tarde."
                            .formatted(maxUploadsPerHour));
        }

        long storedBytes = repository.sumSizeBytesByOwnerId(ownerId);
        long quotaBytes = storageQuotaMb * 1024L * 1024L;
        if (storedBytes + incomingBytes > quotaBytes) {
            throw new QuotaExceededException(
                    "Cota de armazenamento de %d MB atingida.".formatted(storageQuotaMb));
        }
    }

    /** Descarta parametros (";charset=...") e normaliza caixa antes de comparar. */
    private String normalizeContentType(String contentType) {
        if (isNull(contentType) || contentType.isBlank()) {
            return null;
        }
        int separator = contentType.indexOf(';');
        String base = separator < 0 ? contentType : contentType.substring(0, separator);
        return base.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Nome no bucket: UUID novo + extensao SANITIZADA do arquivo original. A
     * extensao vem do cliente, entao so letras e digitos passam — sem isso um
     * nome como {@code "a.jpg/../x"} viraria prefixo de caminho no bucket.
     */
    private String extractStoredName(String originalName) {
        String extension = "";
        if (nonNull(originalName) && originalName.contains(".")) {
            String raw = originalName.substring(originalName.lastIndexOf(".") + 1);
            if (raw.matches("[A-Za-z0-9]{1,10}")) {
                extension = "." + raw.toLowerCase(Locale.ROOT);
            }
        }
        return java.util.UUID.randomUUID() + extension;
    }
}
