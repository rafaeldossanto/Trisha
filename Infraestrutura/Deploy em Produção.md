---
tags: [infra, deploy]
repo: TrishaInfra
atualizado: 2026-07-25
---

# Deploy em Produção

Repositório `trilha-infra` → `TrishaInfra`. **Estado: dormente** — nenhuma VPS
contratada. O compose e o workflow estão prontos, aguardando servidor.

## Topologia de rede (defesa em profundidade)

Duas redes Docker:

```
        internet
            │
            ▼
      ┌───────────┐   rede "edge"
      │    BFF    │ :8090          ← única porta HTTP publicada
      └─────┬─────┘
            │ rede "internal"
   ┌────────┼────────┬─────────┬────────┐
   ▼        ▼        ▼         ▼        ▼
cadastro   app      loc      midia    postgres
                                      redis, minio
```

- Serviços internos **não publicam porta**. O mundo externo não os alcança, então o
  rate limit do [[BFF]] **não é contornável** batendo direto no serviço.
- O **Mosquitto (1883)** é a exceção: devices publicam telemetria de fora.
- A rede `edge` existe para receber um reverse proxy (Caddy/Traefik) que enxergue
  **apenas** o BFF.

## Serviços do compose

| Container | Imagem | Publica porta |
|---|---|---|
| postgres | `postgres:18` | não |
| redis | `redis:8-alpine` | não |
| minio | `minio/minio` | não |
| mosquitto | `eclipse-mosquitto:2` | **1883** |
| cadastro, app, loc, midia | `ghcr.io/rafaeldossanto/trilha-*` | não |
| **bff** | `ghcr.io/rafaeldossanto/trilha-bff` | **8090** |

> [!note] O Zeebe saiu em 01/08/2026
> Ele entrou no compose em 25/07 e saiu de vez uma semana depois, junto com o
> Camunda. O cadastro não depende mais de broker externo — ver
> [[Remoção do Camunda]]. Isso derruba ~1,2 GB do requisito de memória.

## Teto de memória

Todo container tem `mem_limit`, e as cinco JVMs recebem
`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0`.

| Container | Teto | Por quê |
|---|---|---|
| postgres | 1 GB | três schemas; é quem mais aproveita folga |
| midia | 896 MB | multipart de até 50 MB passa pelo heap |
| loc | 704 MB | consumo MQTT contínuo + PostGIS |
| cadastro · app · bff | 640 MB | carga comparável |
| minio | 512 MB | |
| redis | 256 MB | tudo com TTL curto |
| mosquitto · caddy | 128 MB | |

**Soma: ~5,4 GB dos 8 GB.** A folga é proposital — Postgres e MinIO se apoiam no
page cache do SO, que vive fora do limite do container.

> [!important] Por que isso importa
> Sem `mem_limit`, cada JVM enxerga a RAM inteira da máquina e dimensiona o heap
> por cima dela (`MaxRAMPercentage` default = 25%). Com cinco serviços Java isso
> é overcommit garantido: em operação normal ninguém reclama, mas num pico
> simultâneo o kernel escolhe uma vítima e mata — em geral o Postgres, que é o
> processo mais gordo. Com teto, falta de memória vira erro do processo certo, na
> hora certa. Ver [[Escalabilidade]].

## Checklist de ativação

1. **VPS** com Docker (4 GB+ de RAM)
2. Usuário `deploy` com chave SSH e no grupo `docker`
3. Criar `~/trilha`
4. Na VPS, uma vez:
   - `.env.example` → `.env`, preenchendo senhas
   - chave JWT: `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/jwt-private.pem`
   - broker: `mosquitto_passwd -c mosquitto/passwd loc-consumer` e
     `acl.example` → `acl`
   - `MQTT_USERNAME`/`MQTT_PASSWORD` no `.env` com **as mesmas** credenciais do
     `passwd`
   - `CORS_ALLOWED_ORIGINS` com a origem pública do [[App Web]]
5. Secrets no GitHub: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`
6. Actions → Deploy → Run workflow (`todos`)
7. Depois: reverse proxy com TLS e trocar `JWT_ISSUER`/`EMAIL_CONFIRMACAO_URL` para
   o domínio real

## Armadilhas de produção

> [!danger] O Cadastro aborta o boot sem a chave RSA
> Comportamento **intencional**: em prod, sem `JWT_RSA_PRIVATE_KEY_PATH` válido o
> serviço não sobe. Melhor falhar no boot do que assinar token com chave de dev.

> [!warning] `SERVER_FORWARD_HEADERS_STRATEGY`
> Mantenha `none` enquanto o BFF estiver exposto direto. Só troque para `framework`
> **depois** de existir o reverse proxy — senão um cliente forja `X-Forwarded-For` e
> escapa do rate limit.

> [!warning] O broker recusa conexão anônima
> `allow_anonymous false` no `mosquitto.conf` de prod. Sem `MQTT_USERNAME`/
> `MQTT_PASSWORD` no loc, a ingestão GPS silenciosamente não funciona.

## Rollback

As imagens têm tag `sha-<commit>` além de `latest`. Para voltar: trocar a tag no
compose pelo sha anterior e rodar o deploy de novo.

Ver [[CI e CD]] para como as imagens são publicadas, e [[Escalabilidade]] para os
limites desta topologia.
