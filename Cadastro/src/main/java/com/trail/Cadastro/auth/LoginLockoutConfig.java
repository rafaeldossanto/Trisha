package com.trail.Cadastro.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoginLockoutProperties.class)
public class LoginLockoutConfig {
}
