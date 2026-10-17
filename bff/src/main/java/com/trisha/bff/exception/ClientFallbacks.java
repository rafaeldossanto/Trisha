package com.trisha.bff.exception;

import lombok.experimental.UtilityClass;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Traducao de falha nos fallbacks dos clients: erro de NEGOCIO do downstream
 * (4xx — "usuario ja tem sessao", "sem acesso") propaga com o status real;
 * so falha de infraestrutura (I/O, 5xx, circuito aberto) vira 503. Sem isso,
 * todo 4xx chegava ao app como "servico indisponivel".
 */
@UtilityClass
public class ClientFallbacks {

    public static RuntimeException unavailable(Throwable t, String message) {
        if (t instanceof HttpClientErrorException clientError) {
            return clientError;
        }
        return new ServiceUnavailableException(message);
    }
}
