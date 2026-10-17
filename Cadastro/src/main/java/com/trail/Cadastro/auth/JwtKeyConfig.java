package com.trail.Cadastro.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Chave RSA que assina o token da aplicacao e a fonte do JWKS publicado em
 * {@code /oauth2/jwks}. Os demais servicos validam os tokens buscando essa
 * chave publica (jwk-set-uri), sem segredo compartilhado.
 *
 * <p>A chave e carregada de um arquivo PEM (PKCS#8). O {@code kid} e derivado
 * do SHA-256 da chave publica, garantindo estabilidade entre restarts — ao
 * contrario da geracao em memoria, um restart nao invalida tokens em circulacao.
 *
 * <p><b>Fail closed.</b> A chave e SEMPRE obrigatoria via
 * {@code JWT_RSA_PRIVATE_KEY_PATH}. A unica excecao e o par de profiles de
 * desenvolvimento ({@code dev}/{@code test}), onde cai na chave bundled em
 * {@code classpath:keys/dev-private-key.pem} — que esta versionada num
 * repositorio PUBLICO e, portanto, nao e segredo nenhum.
 *
 * <p>A inversao e deliberada: antes o fallback valia para qualquer ambiente que
 * nao fosse explicitamente {@code prod}, entao um deploy sem
 * {@code SPRING_PROFILES_ACTIVE} definido (um {@code docker run} manual, um
 * profile escrito errado, um {@code staging}) subia normalmente assinando tokens
 * com a chave publica do GitHub — qualquer pessoa poderia forjar o JWT de
 * qualquer usuario. Agora o caminho perigoso exige um profile de dev explicito;
 * o silencio aborta o boot.
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    private static final String[] DEVELOPMENT_PROFILES = {"dev", "test"};

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public JwtKeyConfig(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @Bean
    public RSAKey rsaKey() throws JOSEException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = loadPem();
        RSAKey rsaKey = buildRsaKey(pem);
        log.info("Chave RSA carregada (kid={})", rsaKey.getKeyID());
        return rsaKey;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder appJwtDecoder(RSAKey rsaKey) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }

    // --- helpers ---------------------------------------------------------

    private String loadPem() throws IOException {
        String path = jwtProperties.getRsaPrivateKeyPath();
        if (nonNull(path) && !path.isBlank()) {
            log.info("Carregando chave RSA de arquivo: {}", path);
            return Files.readString(Path.of(path));
        }
        // Sem caminho configurado: a chave bundled do classpath e de DEV e esta
        // versionada num repositorio PUBLICO. Assinar tokens com ela permite a
        // qualquer um forja-los, entao ela so vale sob um profile de dev
        // EXPLICITO. Qualquer outro caso — inclusive nenhum profile ativo —
        // aborta o boot em vez de seguir com uma chave conhecida.
        if (!isDevelopmentProfile()) {
            throw new IllegalStateException(
                    "JWT_RSA_PRIVATE_KEY_PATH nao definida. Aponte para o PEM (PKCS#8) da "
                    + "chave RSA privada. A chave bundled em classpath:keys/dev-private-key.pem "
                    + "esta versionada num repositorio publico e so pode ser usada com um dos "
                    + "profiles de desenvolvimento ativo (" + String.join(", ", DEVELOPMENT_PROFILES)
                    + "). Profiles ativos agora: "
                    + Arrays.toString(environment.getActiveProfiles()) + ".");
        }
        // Dev fallback: chave bundled no classpath (nao e um segredo real)
        log.warn("Profile de desenvolvimento {} ativo e JWT_RSA_PRIVATE_KEY_PATH nao definida — "
                        + "assinando tokens com a chave PUBLICA do repositorio "
                        + "(classpath:keys/dev-private-key.pem). Qualquer pessoa pode forjar tokens "
                        + "nesta instancia; jamais exponha este processo na internet.",
                Arrays.toString(environment.getActiveProfiles()));
        try (InputStream is = getClass().getResourceAsStream("/keys/dev-private-key.pem")) {
            if (isNull(is)) {
                throw new IllegalStateException(
                        "Chave RSA de dev nao encontrada em classpath:keys/dev-private-key.pem. "
                        + "Defina JWT_RSA_PRIVATE_KEY_PATH ou inclua o arquivo no classpath.");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private RSAKey buildRsaKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String b64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] derBytes = Base64.getDecoder().decode(b64);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) kf.generatePrivate(new PKCS8EncodedKeySpec(derBytes));
        RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(stableKid(publicKey))
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }

    /** Producao e sinalizada pelos profiles Spring "prod"/"producao"/"production". */
    /**
     * Unicos profiles em que a chave bundled do repositorio pode assinar tokens.
     * Nao inclui o caso "nenhum profile ativo" de proposito — e justamente o
     * cenario de um deploy mal configurado.
     */
    private boolean isDevelopmentProfile() {
        for (String profile : environment.getActiveProfiles()) {
            for (String allowed : DEVELOPMENT_PROFILES) {
                if (profile.equalsIgnoreCase(allowed)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String stableKid(RSAPublicKey publicKey) throws NoSuchAlgorithmException {
        byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256).substring(0, 16);
    }
}
