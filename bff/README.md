# BFF (Backend for Frontend)

Camada de **borda** da Trilha: ponto único de entrada do app mobile. Agrega e roteia as chamadas para os microserviços (Cadastro, APP, loc, midia), cacheia leituras no Redis, propaga a identidade (Bearer) e a correlação (`X-Trace-Id`) downstream, e protege os serviços com *circuit breaker* + *retry*.

- **Porta:** `8090`
- **Pacote raiz:** `com.trisha.bff`
- **Infra:** Redis (cache) na porta `6379`

## O que faz

- **Agregação**: ex. a "tela de aventura" (`GET /bff/aventuras/{id}/detalhe`) junta aventura + caminhos + mídias numa única resposta, evitando várias chamadas sequenciais do app.
- **Roteamento** para os 4 serviços via `RestClient` dedicados (um por downstream).
- **Cache** (Redis, TTL configurável): leituras cacheadas; escritas invalidam as entradas afetadas.
- **Resource server da borda**: valida o Bearer (JWKS do Cadastro) e o **propaga** a cada chamada downstream, junto do `X-Trace-Id`.
- **Resiliência** (Resilience4j): *circuit breaker* + *retry* por downstream (`app`, `cadastro`, `localizacao`, `midia`), com *fallbacks* (ex.: listas vazias quando o serviço está fora).
- **Orquestração** de regra de borda: ex. ao finalizar um caminho, busca a distância real no serviço de localização antes de finalizar no APP.

## Stack

Spring Boot 4.0.6 · Java 21 · Spring WebMVC (`RestClient`) · Spring Cache + Spring Data Redis · **Resilience4j** 2.3.0 · OAuth2 Resource Server · Actuator · springdoc-openapi · Lombok · logs JSON.

## Infra (compose.yaml)

| Serviço | Imagem | Porta |
|---|---|---|
| Redis | `redis:7-alpine` | `6379` |

Em **dev**, `spring-boot-docker-compose` sobe o Redis. Os downstreams (Cadastro/APP/loc/midia) precisam estar no ar nas portas padrão.

## Como rodar

```bash
export JAVA_HOME=/caminho/para/jdk-21   # requer JDK 21

# variáveis (com defaults de localhost):
#   REDIS_HOST/PORT, JWKS_URI, CACHE_TTL_SEGUNDOS,
#   SERVICO_CADASTRO_URL (8080), SERVICO_APP_URL (8081),
#   SERVICO_LOCALIZACAO_URL (8082), SERVICO_MIDIA_URL (8083)
./gradlew bootRun
```

Swagger UI: `http://localhost:8090/swagger-ui.html`. Health: `http://localhost:8090/actuator/health`.

## Principais grupos de rotas (prefixo `/bff`)

| Prefixo | Encaminha para |
|---|---|
| `/bff/usuarios`, `/bff/auth` | Cadastro (CRUD, login social, dev-login, confirmar email) |
| `/bff/aventuras`, `/bff/caminhos`, `/bff/regioes`, `/bff/pontos-interesse`, `/bff/midias` | APP |
| `/bff/amizades`, `/bff/seguidores` | APP (social) |
| `/bff/localizacao` | loc (sessões e pontos GPS) |

## Testes

```bash
./gradlew test             # unitários: clients (MockRestServiceServer), services (Mockito), controllers
./gradlew integrationTest  # integração com Redis real (Testcontainers)
```

## Convenções

Identificadores do código em **inglês**; **JSON e rotas em português** (via `@JsonProperty`). A paginação segue o formato `Page` do Spring Data (`content`/`number`/`size`/`totalElements`/`totalPages`). Nomes de cache (Redis) são internos e permanecem como estão. Correlação por `X-Trace-Id` (gerado na borda e propagado).

> Parte da arquitetura da Trilha: Cadastro (8080) · APP (8081) · loc (8082) · midia (8083) · **BFF (8090)**.
