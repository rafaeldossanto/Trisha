package com.trail.Cadastro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Prazo de confirmacao do cadastro (RegistrationCleanupJob).
@EnableScheduling
// Envio do email de confirmacao fora da requisicao (UserRegisteredListener).
@EnableAsync
// Retentativa do envio SMTP (ConfirmationMailer) — @Retryable nativo do Framework 7.
@EnableResilientMethods
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
