package com.trisha.midia.model.dto.response;

import java.io.InputStream;

/**
 * Binario servido por GET /arquivo/{id}/conteudo, com os metadados que o browser
 * precisa para renderizar. O stream vem do MinIO e e fechado pelo Spring depois
 * de escrever a resposta.
 */
public record FileContent(String contentType, long sizeBytes, InputStream stream) {
}
