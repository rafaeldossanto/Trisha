---
tags: [servico, backend, dominio]
porta: 8081
banco: trilha_app
repo: Trisha
pasta: APP
atualizado: 2026-10-17
---

# Serviço APP

**Porta 8081 · banco `trilha_app` · pacote `com.app.APP`**

O coração do produto. Aventuras, caminhos, pontos de interesse, mídia (metadados),
coleções e todo o grafo social.

## Modelo de domínio

```
Region (coleção)  1──*  Adventure  1──*  Path  1──*  PointOfInterest  1──*  Evidence
   │                        │                              │
 City (embutida)      AdventureParticipant          PointOfInterestUserStatus
                            │
                          Media
```

- **Adventure** — destino, status, visibilidade, região *opcional*
- **Path (caminho)** — o trecho rastreado; tem dono próprio, cor e distância
- **PointOfInterest** — lat/lng, tipo, nível de confiança
- **Evidence** — foto que comprova o ponto (valida distância < 50m)
- **PointOfInterestUserStatus** — marcação *pessoal*: `NO_RADAR` / `NA_MIRA` /
  `CONQUISTADO` + flag de objetivo
- **Region** — a "coleção" de aventuras, com cidades e capa
- **Friendship** / **Follower** — ver [[Visibilidade e Controle de Acesso]]
- **User** — cópia local **imutável** (`@Immutable`) do usuário do
  [[Serviço Cadastro]]

## Endpoints por área

### Aventura (`/aventura`)
`POST` · `GET /{id}` · `GET /usuario/{userId}` · `GET /feed` ·
`PATCH /{id}/status` · `PATCH /{id}/regiao` · `POST /{id}/participante` ·
`DELETE /{id}/participante` (sair) · `DELETE /{id}/participante/{userId}` (remover) ·
`DELETE /{id}`

### Caminho (`/caminho`)
`POST` · `PATCH /{id}/finalizar` · `GET /aventura/{id}` · `GET /usuario/{id}` ·
`GET /{id}/acesso` *(uso interno do loc)* · `GET /descobrir`

### Ponto de interesse (`/ponto-interesse`)
`POST` · `GET /{id}` · `GET /caminho/{pathId}` · `POST /evidencia` ·
`PATCH /{id}/status` · `DELETE /{id}/status` · `PATCH /{id}/objetivo` · `GET /status`

### Região (`/regiao`)
`POST` · `GET` · `GET /descobrir` · `GET /{id}` · `GET /{id}/aventuras` ·
`PUT /{id}` · `DELETE /{id}`

### Social
- **Amizade** (`/amizade`): `POST` · `PATCH /{id}/responder` ·
  `DELETE /{id}/solicitacao` · `DELETE /{id}` · `POST /bloquear` ·
  `DELETE /{id}/bloqueio` · `GET /pendentes` · `GET /enviadas` · `GET /amigos` ·
  `GET /sao-amigos` · `GET /amigos-ids`
- **Seguidor** (`/seguidor`): `POST` · `DELETE` · `GET /seguidores` ·
  `GET /seguindo` · `GET /contadores` · `GET /status` · `GET /segue` ·
  `GET /seguindo-ids`

### Busca (`/usuario`)
`GET /codigo/{userCode}` · `GET /busca` · `GET /resumo` · `GET /resumo-codigo`

### Mídia (`/midia`) — metadados
`POST` · `GET /aventura/{id}` · `GET /caminho/{id}` · `DELETE /{id}`

## Peças importantes

**`AdventureAccessService`** — centraliza toda a autorização de leitura e escrita.
É a peça mais sensível do serviço; ver [[Visibilidade e Controle de Acesso]].

**`AdventureExitService`** — sair de aventura em grupo:
- o **dono não sai** (precisa deletar a aventura);
- `manterDados=true` **move** (não copia) caminhos e mídias para uma aventura
  pessoal privada nova;
- `manterDados=false` faz cascata manual;
- `deleteWithContent` move os dados dos outros participantes antes de apagar — sem
  isso, deletar aventura estourava FK.

**`AdventureMetricsAssembler`** — calcula `participantes` (COUNT) e `duracaoHoras`
(span entre primeiro início e último fim). São **derivados**, sem coluna nova, e o
`buildPage` faz 2 queries em lote para evitar N+1.

**JPA Auditing** — `@CreatedDate`/`@LastModifiedDate` preenchem `criado_em` e
`atualizado_em`. Fica em `config/JpaAuditingConfig`, **não** na `Application`:
na classe da aplicação, os testes `@WebMvcTest` quebravam com *"JPA metamodel must
not be empty"*.

> [!note] Timestamps de negócio continuam manuais
> `Path.startedAt/finishedAt`, `Media.capturedAt`, `Friendship.requestedAt` e
> `AdventureParticipant.joinedAt` **não** são automatizados de propósito — são
> eventos do domínio, não auditoria.

## Dependências

Postgres apenas. Não chama nenhum outro serviço. É consumido pelo [[BFF]] e pelo
[[Serviço Localização]] (só para checagem de visibilidade).
