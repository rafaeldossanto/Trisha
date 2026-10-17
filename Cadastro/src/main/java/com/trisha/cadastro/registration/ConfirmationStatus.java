package com.trisha.cadastro.registration;

public enum ConfirmationStatus {
    PENDENTE,
    CONFIRMADO,
    /**
     * Nenhum fluxo grava este valor hoje. Ele fica porque o status e gravado
     * pela posicao (ordinal): tirar um valor do meio mudaria o significado das
     * linhas que ja estao no banco.
     */
    EXPIRADO
}
