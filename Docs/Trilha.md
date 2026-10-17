---
tags: [moc, trilha]
atualizado: 2026-07-25
---

# Trilha (Trisha)

Rede social de trilhas: o usuário registra **aventuras**, rastreia **caminhos** por
GPS, marca **pontos de interesse** com evidência fotográfica e compartilha tudo com
quem segue. O nome exibido no produto é **Trisha**; os identificadores no código
continuam `Trilha`/`trilha` (ver [[Identidade Visual]]).

> [!info] Como navegar este cofre
> Esta nota é o mapa. Cada link abaixo abre uma nota que, por sua vez, referencia
> as outras — use o **grafo** do Obsidian para ver as dependências reais entre
> serviços.

## Arquitetura

- [[Visão Geral da Arquitetura]] — os 5 microserviços, o BFF e por que essa divisão
- [[Autenticação e Identidade]] — JWT RSA, JWKS, quem emite e quem valida
- [[Visibilidade e Controle de Acesso]] — o modelo de privacidade que atravessa tudo
- [[Comunicação entre Serviços]] — chamadas HTTP internas, circuit breaker, cache
- [[Tempo Real]] — MQTT, WebSocket/STOMP e Redis Pub/Sub

## Serviços

| Serviço | Porta | Banco | Responsabilidade |
|---|---|---|---|
| [[Serviço Cadastro]] | 8080 | `trilha_cadastro` | Contas, login, e-mail, emissão de token |
| [[Serviço APP]] | 8081 | `trilha_app` | Domínio: aventuras, caminhos, social |
| [[Serviço Localização]] | 8082 | `trilha_localizacao` | Sessões GPS, trilhas no mapa |
| [[Serviço Mídia]] | 8083 | `trilha_midia` | Upload de fotos (MinIO) |
| [[BFF]] | 8090 | — | Única porta pública; agrega e protege |

## Front

- [[App Mobile]] — Flutter, Android (`trilha_app`)
- [[App Web]] — Flutter web, desktop (`trilha_web`)
- [[Identidade Visual]] — tema, wordmark, tipografia

## Infraestrutura e operação

- [[Ambientes]] — dev, produção e o que muda entre eles
- [[Banco de Dados e Migrations]] — Flyway, schema base, a pegadinha do `validate`
- [[CI e CD]] — GitHub Actions, imagens no ghcr.io
- [[Deploy em Produção]] — compose, rede segmentada, checklist de ativação
- [[Escalabilidade]] — limites conhecidos e caminhos de crescimento
- [[Segurança]] — postura atual, hardening feito e o que falta

## Contexto

- [[Decisões Deliberadas]] — escolhas que parecem erro mas não são
- [[Linha do Tempo]] — histórico das grandes rodadas de trabalho
- [[Glossário]] — vocabulário do domínio

## Repositórios

Cada serviço tem repositório próprio no GitHub (`rafaeldossanto`):

| Pasta local | Repositório | Notas |
|---|---|---|
| `Cadastro` | `Cadastro` | |
| `APP` | `APP` | |
| `loc` | `loc` | |
| `midia` | `midia` | |
| `bff` | `bfff` | três `f` — erro de digitação preservado |
| `trilha_app` | `frontMobile` | |
| `trilha_web` | `TrishaWeb` | |
| `trilha-infra` | `TrishaInfra` | |

> [!warning] Remotes com nomes diferentes
> Em `bff`, `loc` e `midia` o remote local chama-se **`main`**, não `origin`
> (`git push main master`). Nos demais é `origin`. Detalhe herdado da criação
> dos repos, fácil de esbarrar.
