---
tags: [servico, backend, dominio]
porta: 8081
banco: trisha
repo: Trisha
pasta: APP
atualizado: 2026-10-17
---

# Serviço APP

**Porta 8081 · banco `trisha` (compartilhado com o [[Serviço Cadastro]]) · pacote `com.trisha.app`**

O coração do produto. Aventuras, caminhos, pontos de interesse, mídia (metadados),
coleções e todo o grafo social.

## Estrutura do código

Desde 17/10/2026 o código é organizado **por área de negócio**, não por camada
técnica. Cada pasta tem as próprias entidades, repositórios, services, controller e
mapper, com os DTOs na subpasta `dto/`:

| Pasta | O que mora lá |
|---|---|
| `adventure/` | aventura, participantes, autorização (`AdventureAccessService`), métricas, saída e exclusão (`AdventureMembershipService`) |
| `path/` | caminho |
| `point/` | ponto de interesse, evidência, marcação pessoal, nível de confiança |
| `region/` | coleção (no código continua `Region`) e cidades |
| `media/` | metadados de mídia |
| `social/` | amizade e seguidor |
| `user/` | a visão read-only do usuário e a busca |
| `shared/` | segurança, auditing JPA, tratamento de erro, trace, utilitários |

> [!important] A regra entre áreas
> Para **escrever** numa tabela de outra área, só pelo service dela. Para **ler**,
> pode usar o repositório da outra área direto. A escrita carrega as regras do
> dono; a leitura direta evita ciclo entre beans sem camada extra. Por isso
> `AdventureService` nunca injeta `RegionService` nem `AdventureMembershipService`,
> e `PathService` nunca injeta `PointOfInterestService`. Ver
> [[Decisões Deliberadas]].

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
- **User** — visão **somente leitura** (`@Immutable`) da tabela `usuario` do
  [[Serviço Cadastro]], no mesmo banco (não é uma cópia)

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

**`AdventureAccessService`** — centraliza a autorização de leitura e escrita pela
visibilidade da aventura. É a peça mais sensível do serviço; ver
[[Visibilidade e Controle de Acesso]]. A mesma regra em JPQL mora num lugar só,
`AdventureVisibilityJpql.VISIBLE_TO_OBSERVER`, concatenada nas 5 queries de
listagem e descoberta.

**`AdventureMembershipService`** (antigo `AdventureExitService`) — participantes e
exclusão de aventura:
- adicionar participante (só o dono);
- o **dono não sai** (precisa excluir a aventura);
- `manterDados=true` **move** (não copia) caminhos e mídias para uma aventura
  pessoal privada nova;
- `manterDados=false` faz a cascata manual;
- excluir a aventura move antes os dados dos outros participantes. Sem isso, a
  exclusão estourava FK.

É o único orquestrador entre áreas: a cascata chama `PointOfInterestService`,
`MediaService` e `PathService`, nessa ordem (por causa das FKs), e cada área apaga
ou move os próprios dados.

**Nível de confiança** — `PointOfInterest.confidenceLevel(validadoresDistintos)`:
de 1 a 5, pela descrição e pelo número de usuários distintos com evidência
validada. Fica na entidade porque é uma regra sobre o próprio ponto.

**`AdventureMetricsAssembler`** — calcula `participantes` (COUNT) e `duracaoHoras`
(span entre primeiro início e último fim). São **derivados**, sem coluna nova, e o
`buildPage` faz 2 queries em lote para evitar N+1.

**JPA Auditing** — `@CreatedDate`/`@LastModifiedDate` preenchem `criado_em` e
`atualizado_em`. Fica em `shared/config/JpaAuditingConfig`, **não** na `Application`:
na classe da aplicação, os testes `@WebMvcTest` quebravam com *"JPA metamodel must
not be empty"*.

> [!note] Timestamps de negócio continuam manuais
> `Path.startedAt/finishedAt`, `Media.capturedAt`, `Friendship.requestedAt` e
> `AdventureParticipant.joinedAt` **não** são automatizados de propósito — são
> eventos do domínio, não auditoria.

## Dependências

Postgres apenas, no banco `trisha`, que divide com o [[Serviço Cadastro]]: a
tabela `usuario` é do Cadastro e aqui é só lida. O histórico do Flyway é próprio
(`flyway_schema_history_app`), porque as versões das migrations dos dois se cruzam.
Não chama nenhum outro serviço. É consumido pelo [[BFF]] e pelo
[[Serviço Localização]] (só para checagem de visibilidade).
