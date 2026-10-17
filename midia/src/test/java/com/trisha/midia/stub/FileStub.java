package com.trisha.midia.stub;

import com.trisha.midia.entity.MediaFile;
import com.trisha.midia.model.enums.FileType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Facilitador de testes para MediaFile e MultipartFile.
 */
public final class FileStub {

    public static final String ID = "arquivo-1";
    public static final String BUCKET = "trilha-midia";
    public static final String PUBLIC_BASE_URL = "https://api.trisha.test";
    /** URL permanente servida pelo proprio servico — nao expira (era presigned de 7 dias). */
    public static final String URL = PUBLIC_BASE_URL + "/arquivo/" + ID + "/conteudo";
    public static final String OWNER_ID = "usuario-1";

    private FileStub() {
    }

    public static MediaFile.MediaFileBuilder aFile() {
        return MediaFile.builder()
                .id(ID)
                .originalName("foto.jpg")
                .storedName("uuid-gerado.jpg")
                .type(FileType.FOTO)
                .contentType("image/jpeg")
                .sizeBytes(1024L)
                .bucket(BUCKET)
                .url(URL)
                .ownerId(OWNER_ID)
                .createdAt(LocalDateTime.now());
    }

    /** MultipartFile de imagem valida. */
    public static MultipartFile aPhoto() {
        return new MockMultipartFile(
                "arquivo", "foto.jpg", "image/jpeg", "conteudo-imagem".getBytes());
    }

    /** MultipartFile de video valido. */
    public static MultipartFile aVideo() {
        return new MockMultipartFile(
                "arquivo", "video.mp4", "video/mp4", "conteudo-video".getBytes());
    }

    /** MultipartFile vazio. */
    public static MultipartFile anEmptyFile() {
        return new MockMultipartFile(
                "arquivo", "vazio.jpg", "image/jpeg", new byte[0]);
    }

    /** MultipartFile sem content-type. */
    public static MultipartFile aFileWithoutContentType() {
        return new MockMultipartFile(
                "arquivo", "arquivo.bin", null, "conteudo".getBytes());
    }

    /** SVG com script — e image/*, mas nao pode ser aceito (XSS armazenado). */
    public static MultipartFile anSvg() {
        return new MockMultipartFile("arquivo", "payload.svg", "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes());
    }

    /** Imagem valida com o content-type informado (para testar parametros do header). */
    public static MultipartFile aPhotoWithContentType(String contentType) {
        return new MockMultipartFile(
                "arquivo", "foto.jpg", contentType, "conteudo-imagem".getBytes());
    }

    /** Imagem valida com o nome original informado (para testar sanitizacao). */
    public static MultipartFile aPhotoNamed(String originalName) {
        return new MockMultipartFile(
                "arquivo", originalName, "image/jpeg", "conteudo-imagem".getBytes());
    }
}
