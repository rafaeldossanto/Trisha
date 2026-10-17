package com.trisha.midia.service;

import com.trisha.midia.stub.FileStub;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinioService")
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucket", FileStub.BUCKET);
    }

    @Test
    @DisplayName("getBucket deve retornar o bucket configurado")
    void deveRetornarBucket() {
        assertThat(service.getBucket()).isEqualTo(FileStub.BUCKET);
    }

    @Test
    @DisplayName("upload deve enviar o objeto ao bucket")
    void deveFazerUpload() throws Exception {
        MultipartFile foto = FileStub.aPhoto();

        service.upload("uuid-gerado.jpg", foto);

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("upload nao deve mais assinar URL: o binario e servido pelo proprio servico")
    void naoDeveGerarPresigned() throws Exception {
        // A presigned tinha validade maxima de 7 dias e era persistida (e copiada
        // para Media.url e Region.coverUrl no APP) — passado o prazo, toda foto
        // quebrava. Agora a URL e derivada do id e nao expira.
        service.upload("uuid-gerado.jpg", FileStub.aPhoto());

        verify(minioClient, never()).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    @DisplayName("download deve abrir o objeto do bucket")
    void deveBaixar() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(null);

        service.download("uuid-gerado.jpg");

        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("upload deve propagar o erro real do MinIO, sem mascarar")
    void deveFalharUpload() throws Exception {
        MultipartFile foto = FileStub.aPhoto();
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("conexao recusada"));

        // O servico nao envelopa mais a excecao: a causa real chega intacta ao
        // GlobalExceptionHandler, que loga o stack completo e devolve 500.
        assertThatThrownBy(() -> service.upload("uuid-gerado.jpg", foto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("conexao recusada");
    }

    @Test
    @DisplayName("delete deve remover o objeto do bucket")
    void deveDeletar() throws Exception {
        service.delete("uuid-gerado.jpg");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("delete deve propagar o erro real do MinIO, sem mascarar")
    void deveFalharDelete() throws Exception {
        doThrow(new RuntimeException("objeto inexistente"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThatThrownBy(() -> service.delete("uuid-gerado.jpg"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("objeto inexistente");
    }
}
