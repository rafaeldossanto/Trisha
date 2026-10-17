package com.trisha.midia.service;

import com.trisha.midia.entity.MediaFile;
import com.trisha.midia.exception.QuotaExceededException;
import com.trisha.midia.model.dto.response.FileContent;
import com.trisha.midia.model.dto.response.FileResponse;
import com.trisha.midia.model.enums.FileType;
import com.trisha.midia.repository.MediaFileRepository;
import com.trisha.midia.stub.FileStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService")
class FileServiceTest {

    @Mock
    private MinioService minioService;
    @Mock
    private MediaFileRepository repository;

    @InjectMocks
    private FileService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "publicBaseUrl", FileStub.PUBLIC_BASE_URL);
        ReflectionTestUtils.setField(service, "maxUploadsPerHour", 60);
        ReflectionTestUtils.setField(service, "storageQuotaMb", 2048L);
    }

    @Test
    @DisplayName("upload deve validar, enviar ao MinIO e persistir metadados com o dono")
    void shouldUpload() {
        MultipartFile photo = FileStub.aPhoto();
        when(minioService.getBucket()).thenReturn(FileStub.BUCKET);
        when(repository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponse response = service.upload(photo, FileType.FOTO, FileStub.OWNER_ID);

        assertThat(response.type()).isEqualTo(FileType.FOTO);
        assertThat(response.originalName()).isEqualTo("foto.jpg");
        verify(minioService).upload(anyString(), any(MultipartFile.class));
        verify(repository).save(any(MediaFile.class));
    }

    @Test
    @DisplayName("upload deve devolver URL permanente do proprio servico, sem assinatura nem prazo")
    void shouldReturnPermanentUrl() {
        // Regressao do bug de producao: a URL era uma presigned do MinIO com
        // validade MAXIMA de 7 dias, persistida e copiada para Media.url e
        // Region.coverUrl no APP — passado o prazo, toda foto do app quebrava.
        when(minioService.getBucket()).thenReturn(FileStub.BUCKET);
        when(repository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponse response = service.upload(FileStub.aPhoto(), FileType.FOTO, FileStub.OWNER_ID);

        assertThat(response.url())
                .isEqualTo(FileStub.PUBLIC_BASE_URL + "/arquivo/" + response.id() + "/conteudo")
                .doesNotContain("X-Amz-", "?");
    }

    @Test
    @DisplayName("upload deve persistir a mesma URL permanente que devolve")
    void shouldPersistPermanentUrl() {
        when(minioService.getBucket()).thenReturn(FileStub.BUCKET);
        when(repository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponse response = service.upload(FileStub.aPhoto(), FileType.FOTO, FileStub.OWNER_ID);

        ArgumentCaptor<MediaFile> captor = ArgumentCaptor.forClass(MediaFile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo(response.url());
    }

    @Test
    @DisplayName("upload deve aceitar video com content-type de video")
    void shouldAcceptVideo() {
        MultipartFile video = FileStub.aVideo();
        when(minioService.getBucket()).thenReturn(FileStub.BUCKET);
        when(repository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponse response = service.upload(video, FileType.VIDEO, FileStub.OWNER_ID);

        assertThat(response.type()).isEqualTo(FileType.VIDEO);
    }

    @Test
    @DisplayName("upload deve falhar com arquivo vazio")
    void shouldFailEmptyFile() {
        assertThatThrownBy(() -> service.upload(FileStub.anEmptyFile(), FileType.FOTO, FileStub.OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo vazio");

        verify(minioService, never()).upload(anyString(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("upload deve falhar quando content-type nao identificado")
    void shouldFailWithoutContentType() {
        assertThatThrownBy(() -> service.upload(FileStub.aFileWithoutContentType(), FileType.FOTO, FileStub.OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo do arquivo nao identificado");

        verify(minioService, never()).upload(anyString(), any());
    }

    @Test
    @DisplayName("upload deve falhar quando FOTO recebe arquivo que nao e imagem")
    void shouldFailPhotoWithWrongType() {
        assertThatThrownBy(() -> service.upload(FileStub.aVideo(), FileType.FOTO, FileStub.OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo FOTO nao aceita 'video/mp4'");

        verify(minioService, never()).upload(anyString(), any());
    }

    @Test
    @DisplayName("upload deve falhar quando VIDEO recebe arquivo que nao e video")
    void shouldFailVideoWithWrongType() {
        assertThatThrownBy(() -> service.upload(FileStub.aPhoto(), FileType.VIDEO, FileStub.OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo VIDEO nao aceita 'image/jpeg'");

        verify(minioService, never()).upload(anyString(), any());
    }

    @Test
    @DisplayName("upload deve recusar SVG, mesmo sendo image/*")
    void shouldRejectSvg() {
        // SVG pode carregar JavaScript. Como o binario e servido por um endpoint
        // publico, um SVG aceito viraria XSS armazenado no dominio da API — por
        // isso a validacao e whitelist, e nao um startsWith("image/").
        assertThatThrownBy(() -> service.upload(FileStub.anSvg(), FileType.FOTO, FileStub.OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image/svg+xml");

        verify(minioService, never()).upload(anyString(), any());
    }

    @Test
    @DisplayName("upload deve aceitar content-type com parametro (charset)")
    void shouldAcceptContentTypeWithParameters() {
        when(minioService.getBucket()).thenReturn(FileStub.BUCKET);
        when(repository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponse response = service.upload(
                FileStub.aPhotoWithContentType("image/jpeg; charset=binary"),
                FileType.FOTO, FileStub.OWNER_ID);

        assertThat(response.type()).isEqualTo(FileType.FOTO);
    }

    @Test
    @DisplayName("upload deve sanitizar a extensao vinda do nome original")
    void shouldSanitizeExtension() {
        // Extensao vem do cliente: um nome como "a.jpg/../x" criaria prefixo de
        // caminho dentro do bucket.
        when(minioService.getBucket()).thenReturn(FileStub.BUCKET);
        when(repository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upload(FileStub.aPhotoNamed("foto.jpg/../../evil"), FileType.FOTO, FileStub.OWNER_ID);

        ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
        verify(minioService).upload(storedName.capture(), any(MultipartFile.class));
        assertThat(storedName.getValue()).doesNotContain("/", "..");
    }

    @Test
    @DisplayName("upload deve recusar acima do limite de envios por hora")
    void shouldRejectAboveHourlyLimit() {
        // O upload nao passa pelo BFF, logo nao tem o rate limit por IP da borda.
        when(repository.countByOwnerIdAndCreatedAtAfter(eq(FileStub.OWNER_ID), any()))
                .thenReturn(60L);

        assertThatThrownBy(() -> service.upload(FileStub.aPhoto(), FileType.FOTO, FileStub.OWNER_ID))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("60 envios por hora");

        verify(minioService, never()).upload(anyString(), any());
    }

    @Test
    @DisplayName("upload deve recusar quando estoura a cota de armazenamento")
    void shouldRejectAboveStorageQuota() {
        when(repository.countByOwnerIdAndCreatedAtAfter(eq(FileStub.OWNER_ID), any())).thenReturn(1L);
        when(repository.sumSizeBytesByOwnerId(FileStub.OWNER_ID))
                .thenReturn(2048L * 1024L * 1024L); // cota cheia

        assertThatThrownBy(() -> service.upload(FileStub.aPhoto(), FileType.FOTO, FileStub.OWNER_ID))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("Cota de armazenamento");

        verify(minioService, never()).upload(anyString(), any());
    }

    @Test
    @DisplayName("getById deve retornar arquivo existente")
    void shouldReturnById() {
        when(repository.findById(FileStub.ID)).thenReturn(Optional.of(FileStub.aFile().build()));

        FileResponse response = service.getById(FileStub.ID);

        assertThat(response.id()).isEqualTo(FileStub.ID);
    }

    @Test
    @DisplayName("getById deve derivar a URL da base publica atual, ignorando a persistida")
    void shouldDeriveUrlOnRead() {
        // Registro antigo, gravado quando a URL ainda era uma presigned do MinIO
        // apontando para o host interno. Como a leitura deriva do id, ele volta a
        // funcionar sem migracao de dados — e trocar de dominio tambem nao quebra.
        MediaFile legado = FileStub.aFile()
                .url("http://minio:9000/trilha-midia/uuid-gerado.jpg?X-Amz-Expires=604800")
                .build();
        when(repository.findById(FileStub.ID)).thenReturn(Optional.of(legado));

        FileResponse response = service.getById(FileStub.ID);

        assertThat(response.url()).isEqualTo(FileStub.URL);
    }

    @Test
    @DisplayName("getById deve falhar quando arquivo nao existe")
    void shouldFailGetByIdMissing() {
        when(repository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo nao encontrado");
    }

    @Test
    @DisplayName("download deve devolver o binario com content-type e tamanho")
    void shouldDownload() {
        MediaFile file = FileStub.aFile().build();
        InputStream stream = new ByteArrayInputStream("conteudo-imagem".getBytes());
        when(repository.findById(FileStub.ID)).thenReturn(Optional.of(file));
        when(minioService.download(file.getStoredName())).thenReturn(stream);

        FileContent content = service.download(FileStub.ID);

        assertThat(content.contentType()).isEqualTo("image/jpeg");
        assertThat(content.sizeBytes()).isEqualTo(1024L);
        assertThat(content.stream()).isSameAs(stream);
    }

    @Test
    @DisplayName("download deve falhar quando arquivo nao existe")
    void shouldFailDownloadMissing() {
        when(repository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download("inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo nao encontrado");

        verify(minioService, never()).download(anyString());
    }

    @Test
    @DisplayName("delete deve remover do MinIO e do banco quando e o dono")
    void shouldDelete() {
        MediaFile file = FileStub.aFile().build();
        when(repository.findById(FileStub.ID)).thenReturn(Optional.of(file));

        service.delete(FileStub.ID, FileStub.OWNER_ID);

        verify(minioService).delete(file.getStoredName());
        verify(repository).delete(file);
    }

    @Test
    @DisplayName("delete deve falhar quando nao e o dono")
    void shouldFailDeleteNotOwner() {
        MediaFile file = FileStub.aFile().build();
        when(repository.findById(FileStub.ID)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> service.delete(FileStub.ID, "outro-usuario"))
                .isInstanceOf(com.trisha.midia.exception.ForbiddenException.class)
                .hasMessageContaining("nao e o dono");

        verify(minioService, never()).delete(anyString());
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("delete deve falhar quando arquivo nao existe")
    void shouldFailDeleteMissing() {
        when(repository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("inexistente", FileStub.OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo nao encontrado");

        verify(minioService, never()).delete(anyString());
    }
}
