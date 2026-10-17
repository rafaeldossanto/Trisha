package com.trisha.cadastro.account;

import lombok.experimental.UtilityClass;

/**
 * Codigo publico do usuario ({@code codigoUsuario}): o nome em minusculas e sem
 * espacos, seguido de {@code #} e do proximo valor da sequence do banco.
 */
@UtilityClass
public class UserCode {

    public String of(String name, Long sequence) {
        String formattedName = name.toLowerCase().replaceAll("\\s+", "");
        return formattedName + "#" + sequence;
    }
}
