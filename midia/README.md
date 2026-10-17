# midia (Mídia)

Serviço de **arquivos de mídia** da Trilha. Guarda os binários (fotos/vídeos) em um *object storage* (MinIO) e os metadados no Postgres, devolvendo URLs pré-assinadas. É exposto **diretamente ao front** (o upload não passa pelo BFF), por isso valida o Bearer localmente como resource server.

- **Porta:** `8083`
- **Pacote raiz:** `com.trisha.midia`
- **Banco:** PostgreSQL `trilha_midia` (porta `5434`) · **Storage:** MinIO (`9000`/console `9001`)

## O que faz

- **Upload** de arquivo (multipart, até 50 MB): valida o tipo (`FOTO` espera imagem, `VIDEO` espera vídeo), envia ao MinIO com nome único e persiste os metadados (incluindo o **proprietário**, derivado do token).
- **Consulta** por id (devolve a URL pré-assinada, validade de 7 dias).
- **Exclusão**: apenas o **dono** do arquivo pode apagar (remove do MinIO e do banco).

## Stack

Spring Boot 4.0.6 · Java 21 · Spring Data JPA · MinIO SDK (`io.minio` 8.5.17) · OAuth2 Resource Server · Flyway · Lombok · logs JSON.

## Infra (compose.yaml)

| Serviço | Imagem | Porta |
|---|---|---|
| PostgreSQL | `postgres:16` | `5434` |
| MinIO | `minio/minio:latest` | `9000` (API) / `9001` (console) |

Em **dev**, `spring-boot-docker-compose` sobe Postgres + MinIO automaticamente.

## Como rodar

```bash
export JAVA_HOME=/caminho/para/jdk-21   # requer JDK 21

# variáveis: DB_USERNAME, DB_PASSWORD, MINIO_ACCESS_KEY, MINIO_SECRET_KEY,
#            JWKS_URI (default http://localhost:8080/oauth2/jwks)
./gradlew bootRun
```

Bucket padrão: `trilha-midia` (criado no startup se não existir). Console do MinIO: `http://localhost:9001`.

## Principais endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/arquivo/upload` | upload (multipart: `arquivo`, `tipo`) |
| `GET` | `/arquivo/{id}` | metadados + URL pré-assinada |
| `DELETE` | `/arquivo/{id}` | remove (apenas o dono) |

## Testes

```bash
./gradlew test             # unitários (Mockito) — service + MinIO
./gradlew integrationTest  # integração com Postgres + MinIO reais (Testcontainers)
```

## Convenções

Identificadores do código em **inglês**; **JSON, rotas e colunas do banco em português** (via `@JsonProperty`/`@Column`). Constantes de enum persistidas (`FOTO`/`VIDEO`) são contrato e permanecem como estão. Correlação por `X-Trace-Id`.

> Parte da arquitetura da Trilha: Cadastro (8080) · APP (8081) · loc (8082) · **midia (8083)** · BFF (8090).
