package com.app.APP.model.enums;

/**
 * Progressao pessoal do usuario sobre um ponto de interesse:
 * NO_RADAR (me interessei) -> NA_MIRA (esta nos planos) -> CONQUISTADO (ja fui).
 * A marcacao de objetivo NAO e um status — e uma flag independente na
 * {@code PointOfInterestUserStatus}.
 */
public enum PointStatus {
    NO_RADAR,
    NA_MIRA,
    CONQUISTADO
}
