# APP

Serviço de **domínio principal** da Trilha — concentra o núcleo da rede social de trilhas: aventuras, caminhos, pontos de interesse, evidências, metadados de mídia, amizades, seguidores, regiões (pastas) e busca de usuários. É um *resource server*: recebe o ator autenticado do token (JWT validado pela chave pública do Cadastro), nunca do corpo da requisição.

- **Porta:** `8081`
- **Pacote raiz:** `com.app.APP`
- **Banco:** PostgreSQL `trilha_app` (porta `5432`)

## O que faz

- **Aventuras**: criação, status, visibilidade (`PRIVADA`/`SO_GRUPO`/`PUBLICA`), participantes.
- **Caminhos** (pernas de uma aventura): numeração sequencial por aventura; distância real vem do serviço de localização (orquestrada pelo BFF).
- **Pontos de interesse + evidências**: ponto com tipo e coordenadas; evidências elevam o nível de confiança do ponto (validação por proximidade ≤ 50 m).
- **Mídia (metadados)**: registra a URL do binário (que vive no serviço de Mídia) associada a aventura/caminho.
- **Amizades**: bidirecional, com aceite; **exige seguimento mútuo** para solicitar; bloqueio.
- **Seguidores**: direcional, sem aceite; contadores e status (`sigo`/`meSegue`/`mutuo`).
- **Regiões = "pastas"** de aventuras do usuário: dono, visibilidade própria, lista de cidades e **descoberta** filtrada por visibilidade (a pasta nunca expõe uma aventura que o observador não veria sozinho).
- **Busca de usuários** por `codigoUsuario` (handle público), sem expor email/UUID.

## Stack

Spring Boot 4.0.6 · Java 21 · Spring Data JPA · OAuth2 Resource Server · Flyway · springdoc-openapi (Swagger UI) · Lombok · logs JSON (logstash-logback-encoder).

## Infra (compose.yaml)

| Serviço | Imagem | Porta |
|---|---|---|
| PostgreSQL | `postgres:16` | `5432` |

Em **dev**, `spring-boot-docker-compose` sobe o banco automaticamente.

## Como rodar

```bash
export JAVA_HOME=/caminho/para/jdk-21   # requer JDK 21

# variáveis esperadas: DB_USERNAME, DB_PASSWORD, JWKS_URI (default http://localhost:8080/oauth2/jwks)
./gradlew bootRun
```

Documentação da API: `http://localhost:8081/swagger-ui.html`. Perfil de produção em `application-prod` (`SPRING_PROFILES_ACTIVE=prod`).

## Principais grupos de endpoints

| Prefixo | Domínio |
|---|---|
| `/aventura` | aventuras + participantes |
| `/caminho` | caminhos (pernas) |
| `/ponto-interesse` | pontos de interesse + `/evidencia` |
| `/midia` | metadados de mídia |
| `/amizade` | solicitar/responder/listar amigos e pendentes |
| `/seguidor` | seguir/deixar de seguir, listas, contadores, status |
| `/regiao` | CRUD de pastas + `/descobrir` + `/{id}/aventuras` |
| `/usuario` | busca por código e autocomplete |

## Testes

```bash
./gradlew test             # unitários (Mockito) + testes de controller (Security 7)
./gradlew integrationTest  # integração com Postgres real (Testcontainers)
```

## Convenções

Identificadores do código em **inglês**; **JSON, rotas e colunas do banco em português** (via `@JsonProperty`/`@Column`). O ator vem sempre do token (`UsuarioAutenticado`/claim `codigoUsuario`), não do corpo. Correlação por `X-Trace-Id`.

> Parte da arquitetura da Trilha: Cadastro (8080) · **APP (8081)** · loc (8082) · midia (8083) · BFF (8090).
