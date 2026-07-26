---
tags: [infra]
atualizado: 2026-07-25
---

# Ambientes

Dois ambientes: **dev** (máquina do desenvolvedor) e **prod** (compose numa VPS,
ainda não ativado — ver [[Deploy em Produção]]).

## Dev

### Infra de apoio
```
cd trilha-infra
docker compose -f docker-compose.dev.yml up -d
```

Sobe **Redis** (6379), **MinIO** (9000 + console 9001), **Mosquitto** (1883,
anônimo) e **Zeebe** (26500).

> [!important] O Postgres é NATIVO, fora do compose
> Postgres 18 instalado na máquina, na porta 5432, com os bancos
> `trilha_cadastro`, `trilha_app`, `trilha_localizacao` e `trilha_midia`.
> Não está no `docker-compose.dev.yml` de propósito.

### Serviços
Rodam fora do Docker, na IDE ou por `./gradlew bootRun`, com
`SPRING_PROFILES_ACTIVE=dev`.

> [!warning] JAVA_HOME
> O Gradle exige **JDK 21**. O `PATH` da máquina tem apenas um JRE 8, então é
> preciso `JAVA_HOME=C:\Users\rafae\.jdks\ms-21.0.11` antes de qualquer
> `./gradlew`.

### Fronts
- [[App Mobile]]: emulador `trilha_pixel`; `.env` usa `10.0.2.2` (localhost do host)
- [[App Web]]: `flutter run -d chrome`; `.env` usa `localhost`

## Diferenças entre dev e prod

| Aspecto | Dev | Prod |
|---|---|---|
| `ddl-auto` | `update` (Hibernate cria) | `validate` (só Flyway) |
| Flyway baseline | `baseline-on-migrate: true` | `false` |
| Chave JWT | embarcada no classpath | PEM montado; **sem ela o boot aborta** |
| MQTT | anônimo | usuário e senha obrigatórios |
| CORS | `localhost:*` | domínio real via `CORS_ALLOWED_ORIGINS` |
| Swagger | ligado | desligado |
| Portas | todas expostas | só BFF (8090) e MQTT (1883) |
| Zeebe | compose de dev | serviço no compose de prod |

## Variáveis de ambiente principais

| Variável | Quem usa | Para quê |
|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | todos com banco | credenciais |
| `DDL_AUTO` | todos com banco | `update` ou `validate` |
| `JWKS_URI` | APP, loc, mídia, BFF | validar token |
| `JWT_RSA_PRIVATE_KEY_PATH` | Cadastro | assinar token |
| `ZEEBE_GATEWAY_ADDRESS` | Cadastro | processo de cadastro |
| `REDIS_HOST` | Cadastro, loc, BFF | cache, lockout, pub/sub |
| `MINIO_URL` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | mídia | storage |
| `MQTT_BROKER_URL` / `MQTT_USERNAME` / `MQTT_PASSWORD` | loc | ingestão GPS |
| `CORS_ALLOWED_ORIGINS` | BFF, mídia | liberar o front web |
| `SERVICO_*_URL` | BFF, loc | descoberta de serviço |

Ver [[Banco de Dados e Migrations]] para o que muda no schema entre ambientes.
