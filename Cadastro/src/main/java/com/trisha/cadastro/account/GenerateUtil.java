package com.trisha.cadastro.account;

import lombok.experimental.UtilityClass;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class GenerateUtil {

    public static String makeCode(String name, Long sequence) {
        String formattedName = name.toLowerCase().replaceAll("\\s+", "");
        return formattedName + "#" + sequence;
    }
}