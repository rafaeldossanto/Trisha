package com.trisha.midia.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @SneakyThrows
    public void upload(String storedName, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(storedName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Arquivo {} enviado ao MinIO com sucesso", storedName);
        }
    }

    /**
     * Abre o objeto para leitura. O binario e servido pelo proprio servico
     * (GET /arquivo/{id}/conteudo), entao o bucket permanece privado e a URL
     * publicada nao carrega assinatura nem prazo de validade.
     * <p>
     * Quem chama e dono do stream e precisa fecha-lo.
     */
    @SneakyThrows
    public InputStream download(String storedName) {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(storedName)
                        .build()
        );
    }

    @SneakyThrows
    public void delete(String storedName) {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(storedName)
                        .build()
        );
        log.info("Arquivo {} removido do MinIO", storedName);
    }

    public String getBucket() {
        return bucket;
    }
}
