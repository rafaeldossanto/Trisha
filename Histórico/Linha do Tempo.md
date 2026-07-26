---
tags: [historico]
atualizado: 2026-07-25
---

# Linha do Tempo

As grandes rodadas de trabalho, em ordem cronológica. Serve para entender **por
que** o código está como está.

## Junho / início de julho 2026 — a base

Backend com os cinco serviços e o [[App Mobile]] já bem avançados. Autenticação
ponta a ponta concluída: o [[Serviço Cadastro]] emite JWT RSA, todos os outros
viram *resource server*, o [[BFF]] propaga o Bearer.

Social: amizade bidirecional com aceite, e **seguidores** (unilateral, sem aceite).
Regiões viraram "pasta pessoal de aventuras", com visibilidade própria.

## 05/07 — redesign, mapa colaborativo e ao vivo

Três rodadas no mesmo dia:

1. **Front estilo Instagram**: tema preto, `StatefulShellRoute`, bottom nav de 5
   itens, e a **home virou o mapa** (`HomeMapScreen`) — a visão original do produto.
2. **Descoberta por bbox**: fan-out completo do endpoint, do PostGIS no loc até o
   debounce no front. Ver [[Tempo Real]].
3. **Assistir ao vivo**: WebSocket/STOMP no app. Aqui apareceu um bug importante —
   só o MQTT publicava no Redis, então quem rastreava pelo app nunca aparecia ao
   vivo.

Também: visibilidade da sessão ao vivo virou configurável (`SEGUIDORES` entrou no
enum) e o hardening de leitura por visibilidade.

## 09/07 — Camunda religado e hardening

O maquinário de e-mail/termos **existia mas estava desconectado**: ninguém iniciava
processo, o BPMN não tinha bindings, não havia deploy. Foi religado (ver
[[Serviço Cadastro]]), e o login por senha entrou.

No mesmo período, a validação do [[Serviço APP]] achou os furos descritos em
[[Segurança]] — incluindo o `POST /participante` que furava todo o resto.

## 10/07 — BCrypt

Revoga a decisão de senha em texto plano.

## 15/07 — 403/IDOR e CI/CD

`ForbiddenException` nos cinco serviços, IDOR fechado em `/usuario`. E o pipeline:
Dockerfile multi-stage, GitHub Actions publicando no ghcr.io, repo `TrishaInfra`
criado com o compose de produção. Ver [[CI e CD]].

## 20–23/07 — features e rebrand

- **Status de POI**: `NO_RADAR` / `NA_MIRA` / `CONQUISTADO` + flag de objetivo
- **Sair de aventura em grupo** + métricas derivadas (participantes, duração)
- **Capa de coleção** e rename "pastas" → "coleções"
- **Rebrand** "Trilha" → **"Trisha"** (só o texto visível)
- **Auditoria de segurança**: bbox, metadados de sessão, corridas de `count()+1`
- **Anti-DDoS**: rate limit, lockout, rede segmentada

## 25/07 — a web e a limpeza para produção

O dia mais denso do histórico recente.

### Front web
Criado o [[App Web]] em repositório separado. Side nav responsiva, larguras máximas
por aba, upload por bytes, tela de boot.

### Bug do logo
Descoberto ao rodar o app: o `_PeakLogo` era desenhado **por cima da tela inteira**
(1352×56 em vez de 56×56) por causa do `CrossAxisAlignment.stretch`. Existia também
no [[App Mobile]] desde o rebrand — nunca tinha sido rodado. Corrigido nos dois,
com teste de regressão validado vermelho→verde.

### Wordmark Yellowtail
Escolhido entre 10 candidatas apresentadas visualmente. Fonte embarcada nos dois
repos. Ver [[Identidade Visual]].

### Auditoria de produção — 4 bloqueadores
Uma pergunta simples ("os serviços estão prontos para prod?") revelou que **não**:

1. **Migrations não criavam o schema** em APP, Cadastro e mídia — banco novo com
   `validate` não subiria. Ver [[Banco de Dados e Migrations]].
2. **Zeebe ausente do compose** e endereço fixo em `localhost` — ninguém se
   cadastraria.
3. **CORS só localhost** — o front web seria bloqueado.
4. **loc conectava anônimo** num broker que proíbe anônimo.

Todos corrigidos, mais um `docker-compose.dev.yml` novo.

### Testes vermelhos há semanas
Ao rodar as suítes, descobriu-se que a do [[Serviço APP]] **não compilava desde
21/07**, mascarando 49 falhas de contexto. Cadastro e Mídia também tinham testes
desatualizados. Como o job de publicação de imagem depende do de teste, é provável
que nenhuma imagem válida tenha ido ao ghcr.io. Ver [[CI e CD]].

Os cinco serviços passaram a ter `./gradlew test` verde.

## Pendências abertas

- Sem VPS; deploy dormente ([[Deploy em Produção]])
- Sem TLS ([[Segurança]])
- Front sem telas de cadastro/recuperação ([[Autenticação e Identidade]])
- `integrationTest` (Testcontainers) nunca executado ([[CI e CD]])
- Roadmap: gamificação, trilhas offline, alerta SOS, login social real
