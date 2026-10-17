package com.trisha.app.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Liga o preenchimento automatico de {@code @CreatedDate}/{@code @LastModifiedDate}
 * nas entidades anotadas com {@code AuditingEntityListener}.
 *
 * Fica aqui, e nao na {@code Application}: em {@code @WebMvcTest} a classe da
 * aplicacao e a raiz do contexto, entao o {@code @EnableJpaAuditing} tentava
 * subir a infra de JPA numa fatia que nao carrega entidade nenhuma e o contexto
 * quebrava com "JPA metamodel must not be empty". As fatias web excluem
 * {@code @Configuration} comum do component scan, entao esta classe so entra no
 * contexto completo — onde o metamodel existe de verdade.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
