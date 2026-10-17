---
tags: [referencia]
atualizado: 2026-07-25
---

# Glossário

Vocabulário do domínio. O código usa **inglês** nos identificadores internos e
**português** no contrato REST e no banco — por isso cada termo aparece nas duas
formas.

| Português (contrato/banco) | Inglês (código) | O que é |
|---|---|---|
| **Aventura** | `Adventure` | A unidade principal: uma viagem/expedição a um destino |
| **Caminho** | `Path` | Um trecho rastreado dentro da aventura. Tem dono próprio |
| **Ponto de interesse** | `PointOfInterest` | Lugar marcado no mapa (cachoeira, mirante…) |
| **Evidência** | `Evidence` | Foto que comprova o ponto; valida distância < 50m |
| **Região / Coleção** | `Region` | "Pasta" pessoal que agrupa aventuras |
| **Cidade** | `City` | Sub-item embutido da região (`@Embeddable`) |
| **Amizade** | `Friendship` | Relação bilateral, com aceite |
| **Seguidor** | `Follower` | Relação unilateral, sem aceite |
| **Sessão de rastreamento** | `TrackingSession` | Uma gravação de GPS em andamento |
| **Ponto GPS** | `GpsPoint` | Coordenada individual capturada |
| **Participante** | `AdventureParticipant` | Quem foi adicionado a uma aventura em grupo |

> [!note] "Coleção" é o nome novo de "pasta"
> O rename de 23/07/2026 mudou só o texto da interface. No código continua
> `Region`/`regiao`. Ver [[Decisões Deliberadas]].

## Estados

**Aventura** — `PLANEJADA` · `EM_ANDAMENTO` · `CONCLUIDA`

**Visibilidade da aventura** — `PRIVADA` · `SO_GRUPO` · `PUBLICA`

**Visibilidade da região** — `PRIVADA` · `AMIGOS` · `PUBLICA`

**Visibilidade da sessão** — `PRIVADO` (padrão) · `SEGUIDORES` · `AMIGOS` · `PUBLICO`

**Status do ponto (pessoal)** — `NO_RADAR` · `NA_MIRA` · `CONQUISTADO`

**Amizade** — `PENDENTE` · `ACEITA` · `RECUSADA` · `BLOQUEADA`

**Cadastro** — `PENDENTE` · `ATIVO` · `INATIVO`

Detalhes de quem vê o quê em [[Visibilidade e Controle de Acesso]].

## Termos técnicos

| Termo | Significado aqui |
|---|---|
| **BFF** | *Backend for Frontend* — o [[BFF]], única porta pública |
| **bbox** | *Bounding box*: a área visível do mapa, usada para descobrir trilhas |
| **Decimação** | Reduzir a quantidade de pontos de um caminho preservando a forma |
| **`codigoUsuario`** | Identificador público (`nome#numero`). O `id` interno não circula |
| **traceId** | Correlação de requisição, propagada em `X-Trace-Id` |
| **JWKS** | Endpoint com a chave pública que valida os tokens |
| **Presigned URL** | URL temporária do MinIO (7 dias) para acessar a foto |
| **dev-login** | Atalho de autenticação, só no profile `dev` |
