package com.trail.Cadastro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pool das tarefas @Async (hoje so o envio do email de confirmacao).
 *
 * Existe para nao herdar o executor default do Spring, que cria uma thread nova
 * por chamada e nao impoe teto — num pico de cadastros isso viraria uma thread
 * por requisicao, cada uma segurando uma conexao SMTP.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cadastro-async-");
        // Fila cheia: o envio acontece na thread que publicou em vez de ser
        // descartado. Como quem publica e o callback de AFTER_COMMIT, a resposta
        // HTTP do cadastro ja foi enviada e o usuario nao espera por isso.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
