package com.app.APP.model.enums;

/**
 * Quem enxerga a pasta (regiao) de um usuario. Default seguro: PRIVADA.
 * A visibilidade da regiao controla quem ve a pasta; a visibilidade de cada
 * aventura controla a aventura — a pasta nunca expoe uma aventura que o
 * observador nao veria sozinho.
 */
public enum RegionVisibility {
    PRIVADA,
    AMIGOS,
    PUBLICA
}
