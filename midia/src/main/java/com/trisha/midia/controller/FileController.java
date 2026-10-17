package com.trisha.midia.controller;

import com.trisha.midia.auth.AuthenticatedUser;
import com.trisha.midia.model.dto.response.FileContent;
import com.trisha.midia.model.dto.response.FileResponse;
import com.trisha.midia.model.enums.FileType;
import com.trisha.midia.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/arquivo")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public FileResponse upload(AuthenticatedUser user,
                               @RequestParam("arquivo") MultipartFile file,
                               @RequestParam("tipo") FileType type) {
        return fileService.upload(file, type, user.id());
    }

    @GetMapping("/{id}")
    public FileResponse getById(@PathVariable String id) {
        return fileService.getById(id);
    }

    /**
     * Binario do arquivo. Publico de proposito: e a URL que vai em
     * {@code <img src>} do front, e tag de imagem nao manda Bearer. O id e um
     * UUID opaco — mesmo nivel de protecao da presigned URL que este endpoint
     * substitui, com a diferenca de que aqui nada expira.
     */
    @GetMapping("/{id}/conteudo")
    public ResponseEntity<Resource> content(@PathVariable String id) {
        FileContent content = fileService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.sizeBytes())
                // O conteudo de um id nunca muda (nome no bucket e UUID novo a
                // cada upload), entao o cache pode ser agressivo.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                // O content-type foi declarado pelo cliente no upload. A whitelist
                // do FileService ja barra svg e afins, mas o nosniff fecha o resto:
                // impede o browser de ignorar o header e adivinhar o tipo pelo
                // conteudo (um HTML disfarcado de image/jpeg viraria XSS aqui).
                .header("X-Content-Type-Options", "nosniff")
                // Defesa em profundidade: mesmo que algo executavel chegue ao
                // browser por este endpoint, a CSP nao deixa rodar script nem
                // carregar recurso externo.
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(new InputStreamResource(content.stream()));
    }

    @DeleteMapping("/{id}")
    public void delete(AuthenticatedUser user,
                       @PathVariable String id) {
        fileService.delete(id, user.id());
    }
}
