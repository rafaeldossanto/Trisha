package com.trisha.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "servicos")
@Getter
@Setter
public class ServicosProperties {

    private String cadastro;
    private String app;
    private String localizacao;
    private String midia;
}
