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
                                      redis, minio, zeebe
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
| **zeebe** | `camunda/zeebe:8.5.6` | não |
| mosquitto | `eclipse-mosquitto:2` | **1883** |
| cadastro, app, loc, midia | `ghcr.io/rafaeldossanto/trilha-*` | não |
| **bff** | `ghcr.io/rafaeldossanto/trilha-bff` | **8090** |

> [!important] O Zeebe entrou em 25/07/2026
> Antes ele simplesmente não existia no compose, e o [[Serviço Cadastro]] apontava
> para `localhost:26500` fixo — dentro do container, `localhost` é o próprio
> Cadastro. Resultado: **ninguém conseguiria se cadastrar em produção**. Hoje o
> cadastro depende do Zeebe com `condition: service_healthy`.

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
