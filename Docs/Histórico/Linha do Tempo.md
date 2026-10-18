---
tags: [historico]
atualizado: 2026-10-17
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

## 01/08 — o Camunda sai

Uma provocação sobre custo ("microserviço numa VPS não sai mais caro?") virou
auditoria de dimensionamento. A consolidação de serviços que a pergunta sugeria foi
**rejeitada** (economizaria ~R$ 8/mês e acoplaria o emissor de JWT ao feed), mas a
análise encontrou outra coisa: o Zeebe consumia ~1,2 GB para orquestrar **dois
booleanos e um timeout**, era ponto único de falha do cadastro e guardava estado de
usuário fora do Postgres.

O Camunda saiu inteiro. O fluxo virou `tryActivate()` + `@Scheduled` + evento
assíncrono para o e-mail. A ativação passou a ser um `UPDATE` condicional — some a
janela em que o usuário confirmava o e-mail e ainda não estava `ATIVO`. Ver
[[Remoção do Camunda]].

Achado no caminho: filtrar a expiração por `createdAt` teria matado todo recadastro
de conta inativa em 60 segundos, porque o reaproveitamento preserva a data de
criação. O corte usa `updatedAt`.

## 17/10 — monorepo

Os nove repositórios (cinco serviços, os dois fronts, a infra e este cofre)
viraram o monorepo `rafaeldossanto/Trisha`, com histórico completo e sem
reescrita: os SHAs desta linha do tempo continuam válidos lá. A pasta local
`Work/Trisha` não mudou de lugar, só ganhou um `.git` na raiz.

Junto vieram as branches **`main`** (principal: publica imagem e aceita deploy) e
**`dev`** (trabalho do dia a dia). O CI saiu de dentro de cada pasta para a raiz,
porque o Actions só lê `.github/workflows/` dali. Os cinco `ci.yml` idênticos dos
serviços viraram um workflow reutilizável, cada um filtrado pela própria pasta.
O guard do deploy passou a comparar a pasta com o último run verde em vez de
exigir o run no HEAD. Ver [[CI e CD]] e [[Decisões Deliberadas]].

## 17/10 — Cadastro e APP organizados por área

Os dois serviços maiores trocaram as pastas por camada técnica (`controller/`,
`service/`, `repository/`...) por pastas por área de negócio, e os pacotes raiz
viraram `com.trisha.cadastro` e `com.trisha.app`. loc, midia e bff ficaram como
estavam, por serem pequenos demais para ganhar algo com isso. O contrato não
mudou: rotas, JSON, colunas e mensagens de erro são os mesmos.

Mais do que mover arquivos, cada responsabilidade foi para o dono:
- no Cadastro, só o `UserService` cria e muda o status de um usuário (antes eram 5
  lugares);
- no APP, a saída e a exclusão de aventura passaram a chamar os services de caminho,
  mídia e ponto, em vez de escrever direto em 5 repositórios que não eram dela;
- o nível de confiança foi para a entidade do ponto;
- a regra de visibilidade em JPQL, antes copiada em 5 queries, ficou num lugar só.

A regra entre áreas e os padrões que ficaram de fora estão em
[[Decisões Deliberadas]].

Achado no caminho: no Cadastro, o status `EXPIRADO` do token de confirmação nunca
era gravado, porque a exceção lançada logo depois desfazia a transação. O código
morto saiu; o valor continua no enum, já que o status é gravado pela posição.

A nota de banco também foi corrigida: o cofre ainda dizia "um banco por serviço",
mas Cadastro e APP dividem o `trisha` desde 29/07.

## Pendências abertas

- Sem VPS; deploy dormente ([[Deploy em Produção]])
- Sem TLS ([[Segurança]])
- Front sem telas de cadastro/recuperação ([[Autenticação e Identidade]])
- Repositórios antigos ainda ativos no GitHub: arquivar apontando para o monorepo
- Roadmap: gamificação, trilhas offline, alerta SOS, login social real
